package org.foxtrot.hermetrics.rules;

import org.foxtrot.hermetrics.rules.loader.RuleSetLoader;
import org.foxtrot.hermetrics.rules.loader.RuleTypeRegistry;

import org.foxtrot.hermetrics.canonical.json.CanonicalJsonWriter;
import org.foxtrot.hermetrics.canonical.value.CanonicalString;
import org.foxtrot.hermetrics.canonical.value.CanonicalValue;
import org.foxtrot.hermetrics.canonical.path.PathPattern;
import org.foxtrot.hermetrics.rules.builtin.NumberToleranceRule;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.foxtrot.hermetrics.testutil.Trees.json;

class RuleSetLoaderTest {

    private final RuleSetLoader loader = new RuleSetLoader();

    @Test
    void parsesAllBuiltinRuleTypes() {
        RuleSet rules = loader.parse("""
                {
                  "nullsEqualAbsent": false,
                  "emptyEqualsAbsent": true,
                  "rules": [
                    {"type": "ignore", "path": "**.traceId"},
                    {"type": "mask", "path": "customer.ssn"},
                    {"type": "unordered", "path": "items"},
                    {"type": "numberTolerance", "path": "total", "epsilon": 0.01},
                    {"type": "timeTolerance", "path": "meta.updatedAt", "toleranceMillis": 5000, "epochUnit": "SECONDS"}
                  ]
                }
                """);
        assertThat(rules.nullsEqualAbsent()).isFalse();
        assertThat(rules.emptyEqualsAbsent()).isTrue();
        assertThat(rules.rules()).hasSize(5);
        assertThat(rules.rewriteRules()).hasSize(3);
        assertThat(rules.equivalenceRules()).hasSize(2);
        NumberToleranceRule tolerance = (NumberToleranceRule) rules.equivalenceRules().get(0);
        assertThat(tolerance.epsilon()).isEqualByComparingTo(new BigDecimal("0.01"));
    }

    @Test
    void epsilonAcceptsStringsForExactDecimals() {
        RuleSet rules = loader.parse("""
                {"rules": [{"type": "numberTolerance", "path": "v", "epsilon": "0.001"}]}
                """);
        NumberToleranceRule tolerance = (NumberToleranceRule) rules.equivalenceRules().get(0);
        assertThat(tolerance.epsilon()).isEqualByComparingTo(new BigDecimal("0.001"));
    }

    @Test
    void unknownRuleTypeFailsWithRegisteredTypesListed() {
        assertThatThrownBy(() -> loader.parse("""
                {"rules": [{"type": "fancy", "path": "a"}]}
                """))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("unknown rule type 'fancy'")
                .hasMessageContaining("ignore");
    }

    @Test
    void missingRequiredFieldFails() {
        assertThatThrownBy(() -> loader.parse("""
                {"rules": [{"type": "numberTolerance", "path": "a"}]}
                """))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("epsilon");
    }

    @Test
    void customRuleTypesPlugIn() {
        record Uppercase(PathPattern path) implements TreeRewriteRule {
            @Override
            public Optional<CanonicalValue> rewrite(CanonicalValue node) {
                if (node instanceof CanonicalString s) {
                    return Optional.of(new CanonicalString(s.value().toUpperCase(Locale.ROOT)));
                }
                return Optional.of(node);
            }
        }
        RuleTypeRegistry registry = RuleTypeRegistry.withDefaults()
                .register("uppercase", (path, spec) -> new Uppercase(path));
        RuleSet rules = new RuleSetLoader(registry).parse("""
                {"rules": [{"type": "uppercase", "path": "name"}]}
                """);
        assertThat(CanonicalJsonWriter.write(new Normalizer().normalize(json("{\"name\": \"ada\"}"), rules)))
                .isEqualTo("{\"name\":\"ADA\"}");
    }
}
