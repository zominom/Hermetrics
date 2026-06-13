package org.foxtrot.hermetrics.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public record ApiConfig(int port, String bootstrapServers, String controlTopic,
                        String resultsTopic, String rollupsTopic, String deadLetterTopic,
                        String flinkUiUrl, String bootstrapComparePath, int tailSize,
                        String findingsOffsetReset, Map<String, String> kafkaProperties) {

    public static ApiConfig load(String jsonPath) {
        try {
            JsonNode root = new ObjectMapper().readTree(Files.readString(Path.of(jsonPath)));
            return new ApiConfig(
                    root.path("port").asInt(8080),
                    required(root, "bootstrapServers"),
                    required(root, "controlTopic"),
                    root.path("resultsTopic").asText("hermetrics.verdicts"),
                    root.path("rollupsTopic").asText("hermetrics.rollups"),
                    root.hasNonNull("deadLetterTopic") ? root.get("deadLetterTopic").asText() : null,
                    root.hasNonNull("flinkUiUrl") ? root.get("flinkUiUrl").asText() : null,
                    root.hasNonNull("bootstrapComparePath") ? root.get("bootstrapComparePath").asText() : null,
                    root.path("tailSize").asInt(500),
                    root.path("findingsOffsetReset").asText("latest"),
                    stringMap(root.path("kafkaProperties")));
        } catch (IOException e) {
            throw new IllegalArgumentException("cannot read api config '" + jsonPath + "'", e);
        }
    }

    public Map<String, Object> kafkaBase() {
        Map<String, Object> props = new HashMap<>();
        props.put("bootstrap.servers", bootstrapServers);
        kafkaProperties.forEach(props::put);
        return props;
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
