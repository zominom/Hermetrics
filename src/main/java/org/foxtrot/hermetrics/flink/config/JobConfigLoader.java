package org.foxtrot.hermetrics.flink.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.foxtrot.hermetrics.config.CompareConfig;
import org.foxtrot.hermetrics.config.ConfigLoader;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class JobConfigLoader {

    private static final Set<String> OFFSET_MODES = Set.of("latest", "earliest");

    private final ObjectMapper mapper = new ObjectMapper();
    private final ConfigLoader configLoader;

    public JobConfigLoader(ConfigLoader configLoader) {
        this.configLoader = configLoader;
    }

    public JobConfig load(String json) {
        JsonNode root = readTree(json);

        String compareJson = required(root, "compare").toString();
        CompareConfig compare = configLoader.load(compareJson);
        Set<String> allTopics = compare.topics().keySet();

        JobConfig config = new JobConfig(
                envConfig("main", required(root, "main"), "hermetrics-main", allTopics),
                envConfig("load", required(root, "load"), "hermetrics-load", allTopics),
                controlConfig(required(root, "control")),
                sink(required(root, "results")),
                deadLetters(root),
                rollups(root),
                root.path("stateTtlMillis").asLong(7_200_000L),
                root.hasNonNull("checkpointIntervalMillis") ? root.get("checkpointIntervalMillis").asLong() : null,
                root.hasNonNull("parallelism") ? root.get("parallelism").asInt() : null,
                root.path("emitEqualVerdicts").asBoolean(true),
                compareJson, compare);

        config.main().clusters().forEach(ClusterConfig::physicalToLogical);
        config.load().clusters().forEach(ClusterConfig::physicalToLogical);
        return config;
    }

    private JsonNode readTree(String json) {
        try {
            return mapper.readTree(json);
        } catch (IOException e) {
            throw new IllegalArgumentException("invalid job config JSON", e);
        }
    }

    private static EnvConfig envConfig(String envName, JsonNode node, String defaultGroupId, Set<String> allTopics) {
        if (!node.hasNonNull("clusters")) {
            return new EnvConfig(List.of(cluster(node, defaultGroupId, envName, allTopics)));
        }
        List<ClusterConfig> clusters = new ArrayList<>();
        int index = 0;
        for (JsonNode clusterNode : node.get("clusters")) {
            String defaultName = envName + "-" + index++;
            clusters.add(cluster(clusterNode, defaultGroupId, defaultName, assignedTopics(clusterNode, defaultName)));
        }
        validateTopicAssignment(envName, clusters, allTopics);
        return new EnvConfig(clusters);
    }

    private static ClusterConfig cluster(JsonNode node, String defaultGroupId, String defaultName,
                                         Set<String> topics) {
        String startingOffsets = node.path("startingOffsets").asText("latest");
        if (!OFFSET_MODES.contains(startingOffsets)) {
            throw new IllegalArgumentException(
                    "startingOffsets must be one of " + OFFSET_MODES + ", got '" + startingOffsets + "'");
        }
        return new ClusterConfig(
                node.path("name").asText(defaultName),
                required(node, "bootstrapServers").asText(),
                node.path("groupId").asText(defaultGroupId),
                node.path("topicPrefix").asText(""),
                stringMap(node.path("topicOverrides")),
                startingOffsets,
                stringMap(node.path("properties")),
                topics);
    }

    private static Set<String> assignedTopics(JsonNode clusterNode, String clusterName) {
        JsonNode topicsNode = clusterNode.get("topics");
        if (topicsNode == null || !topicsNode.isArray() || topicsNode.isEmpty()) {
            throw new IllegalArgumentException(
                    "cluster '" + clusterName + "' must list the topics it carries");
        }
        Set<String> topics = new LinkedHashSet<>();
        topicsNode.forEach(topic -> topics.add(topic.asText()));
        return topics;
    }

    private static void validateTopicAssignment(String envName, List<ClusterConfig> clusters,
                                                Set<String> allTopics) {
        Map<String, String> ownerByTopic = new HashMap<>();
        for (ClusterConfig cluster : clusters) {
            for (String topic : cluster.topics()) {
                if (!allTopics.contains(topic)) {
                    throw new IllegalArgumentException("cluster '" + cluster.name() + "' of environment '"
                            + envName + "' lists unknown topic '" + topic + "'");
                }
                String previous = ownerByTopic.put(topic, cluster.name());
                if (previous != null) {
                    throw new IllegalArgumentException("topic '" + topic + "' is assigned to clusters '"
                            + previous + "' and '" + cluster.name() + "' in environment '" + envName + "'");
                }
            }
        }
        Set<String> uncovered = new HashSet<>(allTopics);
        uncovered.removeAll(ownerByTopic.keySet());
        if (!uncovered.isEmpty()) {
            throw new IllegalArgumentException(
                    "environment '" + envName + "' assigns no cluster for topics " + uncovered);
        }
    }

    private static ControlConfig controlConfig(JsonNode node) {
        return new ControlConfig(
                required(node, "bootstrapServers").asText(),
                required(node, "topic").asText(),
                stringMap(node.path("properties")));
    }

    private static SinkConfig deadLetters(JsonNode root) {
        return root.hasNonNull("deadLetters")
                ? sink(root.get("deadLetters"))
                : new SinkConfig("logging", Map.of());
    }

    private static RollupConfig rollups(JsonNode root) {
        if (!root.hasNonNull("rollups")) {
            return null;
        }
        JsonNode node = root.get("rollups");
        return new RollupConfig(
                node.path("windowMillis").asLong(60_000L),
                sink(required(node, "sink")));
    }

    private static SinkConfig sink(JsonNode node) {
        return new SinkConfig(required(node, "type").asText(), stringMap(node.path("options")));
    }

    private static Map<String, String> stringMap(JsonNode node) {
        Map<String, String> map = new HashMap<>();
        node.properties().forEach(entry -> map.put(entry.getKey(), entry.getValue().asText()));
        return map;
    }

    private static JsonNode required(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || value.isNull()) {
            throw new IllegalArgumentException("missing required job config field '" + field + "'");
        }
        return value;
    }
}
