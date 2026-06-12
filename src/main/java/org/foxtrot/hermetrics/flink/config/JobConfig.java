package org.foxtrot.hermetrics.flink.config;

import org.foxtrot.hermetrics.config.CompareConfig;

import java.io.Serializable;

public record JobConfig(EnvConfig main, EnvConfig load, ControlConfig control,
                        SinkConfig results, SinkConfig deadLetters, RollupConfig rollups,
                        long stateTtlMillis, Long checkpointIntervalMillis, Integer parallelism,
                        boolean emitEqualVerdicts, String compareJson,
                        CompareConfig compare) implements Serializable {
}
