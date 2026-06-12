package org.foxtrot.hermetrics.flink.config;

import java.io.Serializable;
import java.util.Map;

public record ControlConfig(String bootstrapServers, String topic,
                            Map<String, String> properties) implements Serializable {

    public ControlConfig {
        properties = Map.copyOf(properties);
    }
}
