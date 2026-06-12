package org.foxtrot.hermetrics.config;

import org.foxtrot.hermetrics.match.MatchPolicy;
import org.foxtrot.hermetrics.rules.RuleSet;

import java.io.Serializable;
import java.util.Map;

public record CompareConfig(MatchPolicy policy, Map<String, RuleSet> ruleSets,
                            Map<String, TopicConfig> topics) implements Serializable {

    public static final String DEFAULT_RULE_SET = "default";

    public CompareConfig {
        ruleSets = Map.copyOf(ruleSets);
        topics = Map.copyOf(topics);
    }

    public RuleSet ruleSetFor(String topic) {
        TopicConfig topicConfig = topics.get(topic);
        String name = topicConfig != null && topicConfig.ruleSet() != null
                ? topicConfig.ruleSet()
                : DEFAULT_RULE_SET;
        return ruleSets.getOrDefault(name, RuleSet.EMPTY);
    }
}
