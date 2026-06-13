package org.foxtrot.hermetrics.config;

import org.foxtrot.hermetrics.canonical.Path;

import java.io.Serializable;
import java.util.List;

public record TopicConfig(String name, Role role, String format,
                          Path guidPath, List<Path> sequencePaths, String ruleSet) implements Serializable {

    public TopicConfig {
        sequencePaths = List.copyOf(sequencePaths);
    }

    public enum Role {
        ENTRY, OUTPUT, BOTH
    }
}
