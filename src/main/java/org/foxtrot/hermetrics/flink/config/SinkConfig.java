package org.foxtrot.hermetrics.flink.config;

import java.io.Serializable;
import java.util.Map;

public record SinkConfig(String type, Map<String, String> options) implements Serializable {

    public SinkConfig {
        options = Map.copyOf(options);
    }

    public String require(String option) {
        String value = options.get(option);
        if (value == null) {
            throw new IllegalArgumentException("sink type '" + type + "' requires option '" + option + "'");
        }
        return value;
    }
}
