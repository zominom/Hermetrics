package org.foxtrot.hermetrics.flink.config;

import java.io.Serializable;

public record RollupConfig(long windowMillis, SinkConfig sink) implements Serializable {
}
