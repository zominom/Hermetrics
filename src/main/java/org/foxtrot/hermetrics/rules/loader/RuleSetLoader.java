package org.foxtrot.hermetrics.rules.loader;

import org.foxtrot.hermetrics.rules.NormalizationRule;
import org.foxtrot.hermetrics.rules.RuleSet;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.foxtrot.hermetrics.canonical.path.PathPattern;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public final class RuleSetLoader {

    private final ObjectMapper mapper = new ObjectMapper();
    private final RuleTypeRegistry ruleTypes;

    public RuleSetLoader() {
        this(RuleTypeRegistry.withDefaults());
    }

    public RuleSetLoader(RuleTypeRegistry ruleTypes) {
        this.ruleTypes = ruleTypes;
    }

    public RuleSet parse(String json) {
        try {
            return parse(mapper.readTree(json));
        } catch (IOException e) {
            throw new IllegalArgumentException("invalid rule set JSON", e);
        }
    }

    public RuleSet parse(JsonNode node) {
        boolean nullsEqualAbsent = node.path("nullsEqualAbsent").asBoolean(true);
        boolean emptyEqualsAbsent = node.path("emptyEqualsAbsent").asBoolean(false);
        List<NormalizationRule> rules = new ArrayList<>();
        for (JsonNode ruleNode : node.path("rules")) {
            rules.add(parseRule(ruleNode));
        }
        return new RuleSet(nullsEqualAbsent, emptyEqualsAbsent, rules);
    }

    private NormalizationRule parseRule(JsonNode ruleNode) {
        RuleFactory factory = ruleTypes.forType(required(ruleNode, "type").asText());
        PathPattern path = PathPattern.parse(required(ruleNode, "path").asText());
        return factory.create(path, ruleNode);
    }

    private static JsonNode required(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || value.isNull()) {
            throw new IllegalArgumentException("missing required rule field '" + field + "' in " + node);
        }
        return value;
    }
}
