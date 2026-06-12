package org.foxtrot.hermetrics.config;

import org.foxtrot.hermetrics.canonical.Path;

import java.io.Serializable;

public record TopicConfig(String name, Role role, String format,
                          Path guidPath, String ruleSet) implements Serializable {

    public enum Role {
        ENTRY, OUTPUT, BOTH
    }
}
