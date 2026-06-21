package org.foxtrot.hermetrics.api;

import java.util.HashMap;
import java.util.Map;

public record TopicEndpoint(String topic, String bootstrapServers, Map<String, String> properties) {

    public Map<String, Object> kafkaBase() {
        Map<String, Object> props = new HashMap<>();
        props.put("bootstrap.servers", bootstrapServers);
        properties.forEach(props::put);
        return props;
    }
}
