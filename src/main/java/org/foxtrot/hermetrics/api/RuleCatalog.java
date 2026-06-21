package org.foxtrot.hermetrics.api;

import org.foxtrot.hermetrics.rules.loader.RuleTypeRegistry;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class RuleCatalog {

    private static final Map<String, List<Param>> BUILTIN_PARAMS = Map.of(
            "ignore", List.of(),
            "mask", List.of(),
            "unordered", List.of(),
            "cast", List.of(new Param("to", "enum:NUMBER,BOOLEAN", true)),
            "numberTolerance", List.of(new Param("epsilon", "number", true)),
            "timeTolerance", List.of(
                    new Param("toleranceMillis", "number", true),
                    new Param("epochUnit", "enum:MILLIS,SECONDS", false)));

    private record Param(String name, String kind, boolean required) {
    }

    private final RuleTypeRegistry registry;

    public RuleCatalog(RuleTypeRegistry registry) {
        this.registry = registry;
    }

    public List<Map<String, Object>> describe() {
        List<Map<String, Object>> types = new ArrayList<>();
        for (String type : registry.types()) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("type", type);
            entry.put("params", BUILTIN_PARAMS.getOrDefault(type, List.of()));
            types.add(entry);
        }
        types.sort((a, b) -> ((String) a.get("type")).compareTo((String) b.get("type")));
        return types;
    }
}
