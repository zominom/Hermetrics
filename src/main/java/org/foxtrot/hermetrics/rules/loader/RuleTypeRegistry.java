package org.foxtrot.hermetrics.rules.loader;

import org.foxtrot.hermetrics.rules.NormalizationRule;

import com.fasterxml.jackson.databind.JsonNode;
import org.foxtrot.hermetrics.canonical.path.PathPattern;
import org.foxtrot.hermetrics.rules.builtin.IgnoreRule;
import org.foxtrot.hermetrics.rules.builtin.MaskRule;
import org.foxtrot.hermetrics.rules.builtin.NumberToleranceRule;
import org.foxtrot.hermetrics.rules.builtin.TimeToleranceRule;
import org.foxtrot.hermetrics.rules.builtin.UnorderedRule;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public final class RuleTypeRegistry {

    private final Map<String, RuleFactory> byType = new HashMap<>();

    public static RuleTypeRegistry withDefaults() {
        return new RuleTypeRegistry()
                .register("ignore", (path, spec) -> new IgnoreRule(path))
                .register("mask", (path, spec) -> new MaskRule(path))
                .register("unordered", (path, spec) -> new UnorderedRule(path))
                .register("numberTolerance", (path, spec) ->
                        new NumberToleranceRule(path, decimal(spec, "epsilon")))
                .register("timeTolerance", RuleTypeRegistry::timeTolerance);
    }

    public RuleTypeRegistry register(String type, RuleFactory factory) {
        byType.put(type, factory);
        return this;
    }

    public java.util.Set<String> types() {
        return java.util.Set.copyOf(byType.keySet());
    }

    public RuleFactory forType(String type) {
        RuleFactory factory = byType.get(type);
        if (factory == null) {
            throw new IllegalArgumentException(
                    "unknown rule type '" + type + "', registered: " + byType.keySet());
        }
        return factory;
    }

    private static NormalizationRule timeTolerance(PathPattern path, JsonNode spec) {
        long tolerance = required(spec, "toleranceMillis").asLong();
        String unit = spec.path("epochUnit").asText("MILLIS").toUpperCase(Locale.ROOT);
        return new TimeToleranceRule(path, tolerance, TimeToleranceRule.EpochUnit.valueOf(unit));
    }

    private static BigDecimal decimal(JsonNode node, String field) {
        JsonNode value = required(node, field);
        return value.isTextual() ? new BigDecimal(value.asText()) : value.decimalValue();
    }

    private static JsonNode required(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || value.isNull()) {
            throw new IllegalArgumentException("missing required rule field '" + field + "' in " + node);
        }
        return value;
    }
}
