package org.foxtrot.hermetrics.flink.config;

import java.io.Serializable;
import java.util.List;

public record EnvConfig(List<ClusterConfig> clusters) implements Serializable {

    public EnvConfig {
        clusters = List.copyOf(clusters);
        if (clusters.isEmpty()) {
            throw new IllegalArgumentException("an environment needs at least one cluster");
        }
    }
}
