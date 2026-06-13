package org.foxtrot.hermetrics.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.foxtrot.hermetrics.canonical.path.Path;
import org.foxtrot.hermetrics.match.MatchPolicy;
import org.foxtrot.hermetrics.rules.RuleSet;
import org.foxtrot.hermetrics.rules.loader.RuleSetLoader;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class ConfigLoader {

    private final ObjectMapper mapper = new ObjectMapper();
    private final RuleSetLoader ruleSetLoader;

    public ConfigLoader() {
        this(new RuleSetLoader());
    }

    public ConfigLoader(RuleSetLoader ruleSetLoader) {
        this.ruleSetLoader = ruleSetLoader;
    }

    public CompareConfig load(String json) {
        JsonNode root = readTree(json);
        MatchPolicy policy = parsePolicy(root.path("policy"));
        Map<String, RuleSet> ruleSets = parseRuleSets(root.path("ruleSets"));
        Map<String, TopicConfig> topics = parseTopics(root.path("topics"), ruleSets);
        return new CompareConfig(policy, ruleSets, topics);
    }

    private JsonNode readTree(String json) {
        try {
            return mapper.readTree(json);
        } catch (IOException e) {
            throw new IllegalArgumentException("invalid config JSON", e);
        }
    }

    private static MatchPolicy parsePolicy(JsonNode node) {
        MatchPolicy defaults = MatchPolicy.defaults();
        return new MatchPolicy(
                node.path("quietMillis").asLong(defaults.quietMillis()),
                node.path("maxWaitMillis").asLong(defaults.maxWaitMillis()),
                node.path("strictIntermediates").asBoolean(defaults.strictIntermediates()),
                MatchPolicy.CohortMode.valueOf(
                        node.path("cohortMode").asText(defaults.cohortMode().name()).toUpperCase(Locale.ROOT)));
    }

    private Map<String, RuleSet> parseRuleSets(JsonNode node) {
        Map<String, RuleSet> ruleSets = new HashMap<>();
        node.properties().forEach(entry -> ruleSets.put(entry.getKey(), ruleSetLoader.parse(entry.getValue())));
        return ruleSets;
    }

    private static Map<String, TopicConfig> parseTopics(JsonNode node, Map<String, RuleSet> ruleSets) {
        Map<String, TopicConfig> topics = new HashMap<>();
        for (JsonNode topicNode : node) {
            TopicConfig topic = parseTopic(topicNode, ruleSets);
            if (topics.put(topic.name(), topic) != null) {
                throw new IllegalArgumentException("duplicate topic in config: " + topic.name());
            }
        }
        if (topics.isEmpty()) {
            throw new IllegalArgumentException("config defines no topics");
        }
        return topics;
    }

    private static TopicConfig parseTopic(JsonNode node, Map<String, RuleSet> ruleSets) {
        String name = required(node, "name").asText();
        String ruleSet = node.hasNonNull("ruleSet") ? node.get("ruleSet").asText() : null;
        if (ruleSet != null && !ruleSets.containsKey(ruleSet)) {
            throw new IllegalArgumentException("topic '" + name + "' references unknown rule set '" + ruleSet + "'");
        }
        return new TopicConfig(
                name,
                TopicConfig.Role.valueOf(node.path("role").asText("OUTPUT").toUpperCase(Locale.ROOT)),
                required(node, "format").asText(),
                Path.parse(required(node, "guidPath").asText()),
                sequencePaths(node.get("sequencePath")),
                ruleSet);
    }

    private static List<Path> sequencePaths(JsonNode node) {
        if (node == null || node.isNull()) {
            return List.of();
        }
        if (node.isTextual()) {
            return List.of(Path.parse(node.asText()));
        }
        List<Path> paths = new ArrayList<>();
        node.forEach(path -> paths.add(Path.parse(path.asText())));
        if (paths.isEmpty()) {
            throw new IllegalArgumentException("sequencePath must be a path or a non-empty array of paths");
        }
        return paths;
    }

    private static JsonNode required(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || value.isNull()) {
            throw new IllegalArgumentException("missing required field '" + field + "' in " + node);
        }
        return value;
    }
}
