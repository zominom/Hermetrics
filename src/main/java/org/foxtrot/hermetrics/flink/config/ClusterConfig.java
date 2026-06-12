package org.foxtrot.hermetrics.flink.config;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public record ClusterConfig(String name, String bootstrapServers, String groupId,
                            String topicPrefix, Map<String, String> topicOverrides,
                            String startingOffsets, Map<String, String> properties,
                            Set<String> topics) implements Serializable {

    public ClusterConfig {
        topicOverrides = Map.copyOf(topicOverrides);
        properties = Map.copyOf(properties);
        topics = Set.copyOf(topics);
    }

    public String physicalTopic(String logicalTopic) {
        return topicOverrides.getOrDefault(logicalTopic, topicPrefix + logicalTopic);
    }

    public Map<String, String> physicalToLogical() {
        Map<String, String> mapping = new HashMap<>();
        for (String logical : topics) {
            String previous = mapping.put(physicalTopic(logical), logical);
            if (previous != null) {
                throw new IllegalArgumentException("topics '" + previous + "' and '" + logical
                        + "' map to the same physical topic '" + physicalTopic(logical)
                        + "' on cluster '" + name + "'");
            }
        }
        return mapping;
    }
}
