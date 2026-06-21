package org.foxtrot.hermetrics.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public record ApiConfig(int port, TopicEndpoint control, TopicEndpoint results,
                        TopicEndpoint rollups, TopicEndpoint deadLetters,
                        String flinkUiUrl, String bootstrapComparePath, int tailSize,
                        String findingsOffsetReset, String findingsStoreType,
                        Map<String, String> findingsStoreOptions) {

    public static ApiConfig load(String jsonPath) {
        try {
            JsonNode root = new ObjectMapper().readTree(Files.readString(Path.of(jsonPath)));
            String defaultBootstrap = optionalText(root, "bootstrapServers");
            Map<String, String> defaultProps = stringMap(root.path("kafkaProperties"));
            return new ApiConfig(
                    root.path("port").asInt(8080),
                    endpoint(root, "controlTopic", null, defaultBootstrap, defaultProps),
                    endpoint(root, "resultsTopic", "hermetrics.verdicts", defaultBootstrap, defaultProps),
                    endpoint(root, "rollupsTopic", "hermetrics.rollups", defaultBootstrap, defaultProps),
                    optionalEndpoint(root, "deadLetterTopic", defaultBootstrap, defaultProps),
                    optionalText(root, "flinkUiUrl"),
                    optionalText(root, "bootstrapComparePath"),
                    root.path("tailSize").asInt(500),
                    root.path("findingsOffsetReset").asText("latest"),
                    root.path("findingsStore").path("type").asText("kafka-tail"),
                    stringMap(root.path("findingsStore").path("options")));
        } catch (IOException e) {
            throw new IllegalArgumentException("cannot read api config '" + jsonPath + "'", e);
        }
    }

    private static TopicEndpoint optionalEndpoint(JsonNode root, String field, String defaultBootstrap,
                                                  Map<String, String> defaultProps) {
        JsonNode node = root.get(field);
        return node == null || node.isNull() ? null : endpoint(root, field, null, defaultBootstrap, defaultProps);
    }

    private static TopicEndpoint endpoint(JsonNode root, String field, String defaultTopic,
                                          String defaultBootstrap, Map<String, String> defaultProps) {
        JsonNode node = root.get(field);
        if (node == null || node.isNull()) {
            if (defaultTopic == null) {
                throw new IllegalArgumentException("missing required api config field '" + field + "'");
            }
            return resolve(field, defaultTopic, null, null, defaultBootstrap, defaultProps);
        }
        if (node.isTextual()) {
            return resolve(field, node.asText(), null, null, defaultBootstrap, defaultProps);
        }
        Map<String, String> ownProps = node.has("properties") ? stringMap(node.path("properties")) : null;
        return resolve(field, required(node, "topic"), optionalText(node, "bootstrapServers"),
                ownProps, defaultBootstrap, defaultProps);
    }

    private static TopicEndpoint resolve(String field, String topic, String bootstrap,
                                         Map<String, String> properties, String defaultBootstrap,
                                         Map<String, String> defaultProps) {
        String resolvedBootstrap = bootstrap != null ? bootstrap : defaultBootstrap;
        if (resolvedBootstrap == null) {
            throw new IllegalArgumentException(
                    "api config topic '" + field + "' has no bootstrapServers and no top-level default");
        }
        return new TopicEndpoint(topic, resolvedBootstrap, properties != null ? properties : defaultProps);
    }

    private static String optionalText(JsonNode node, String field) {
        return node.hasNonNull(field) ? node.get(field).asText() : null;
    }

    private static String required(JsonNode node, String field) {
        if (!node.hasNonNull(field)) {
            throw new IllegalArgumentException("missing required api config field '" + field + "'");
        }
        return node.get(field).asText();
    }

    private static Map<String, String> stringMap(JsonNode node) {
        Map<String, String> map = new HashMap<>();
        node.properties().forEach(entry -> map.put(entry.getKey(), entry.getValue().asText()));
        return map;
    }
}
