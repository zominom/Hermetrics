package org.foxtrot.hermetrics.rules;

import java.io.Serializable;
import java.util.List;

public final class RuleSet implements Serializable {

    public static final RuleSet EMPTY = new RuleSet(true, false, List.of());

    private final boolean nullsEqualAbsent;
    private final boolean emptyEqualsAbsent;
    private final List<NormalizationRule> rules;
    private final List<TreeRewriteRule> rewriteRules;
    private final List<EquivalenceRule> equivalenceRules;

    public RuleSet(boolean nullsEqualAbsent, boolean emptyEqualsAbsent, List<NormalizationRule> rules) {
        this.nullsEqualAbsent = nullsEqualAbsent;
        this.emptyEqualsAbsent = emptyEqualsAbsent;
        this.rules = List.copyOf(rules);
        this.rewriteRules = this.rules.stream()
                .filter(TreeRewriteRule.class::isInstance)
                .map(TreeRewriteRule.class::cast)
                .toList();
        this.equivalenceRules = this.rules.stream()
                .filter(EquivalenceRule.class::isInstance)
                .map(EquivalenceRule.class::cast)
                .toList();
    }

    public static RuleSet of(NormalizationRule... rules) {
        return new RuleSet(true, false, List.of(rules));
    }

    public boolean nullsEqualAbsent() {
        return nullsEqualAbsent;
    }

    public boolean emptyEqualsAbsent() {
        return emptyEqualsAbsent;
    }

    public List<NormalizationRule> rules() {
        return rules;
    }

    public List<TreeRewriteRule> rewriteRules() {
        return rewriteRules;
    }

    public List<EquivalenceRule> equivalenceRules() {
        return equivalenceRules;
    }
}
