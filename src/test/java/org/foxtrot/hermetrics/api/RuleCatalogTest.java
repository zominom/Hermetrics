package org.foxtrot.hermetrics.api;

import org.foxtrot.hermetrics.rules.builtin.IgnoreRule;
import org.foxtrot.hermetrics.rules.loader.RuleTypeRegistry;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class RuleCatalogTest {

    private static List<String> names(List<Map<String, Object>> described) {
        return described.stream().map(entry -> (String) entry.get("type")).toList();
    }

    @Test
    void describesBuiltinTypesSortedWithParams() {
        List<Map<String, Object>> described = new RuleCatalog(RuleTypeRegistry.withDefaults()).describe();
        assertThat(names(described))
                .containsExactly("ignore", "mask", "numberTolerance", "timeTolerance", "unordered");

        Map<String, Object> numberTolerance = described.stream()
                .filter(entry -> entry.get("type").equals("numberTolerance"))
                .findFirst().orElseThrow();
        assertThat((List<?>) numberTolerance.get("params")).hasSize(1);

        Map<String, Object> ignore = described.stream()
                .filter(entry -> entry.get("type").equals("ignore"))
                .findFirst().orElseThrow();
        assertThat((List<?>) ignore.get("params")).isEmpty();
    }

    @Test
    void includesCustomRegisteredTypes() {
        RuleTypeRegistry registry = RuleTypeRegistry.withDefaults()
                .register("uppercase", (path, spec) -> new IgnoreRule(path));
        assertThat(names(new RuleCatalog(registry).describe())).contains("uppercase");
    }
}
