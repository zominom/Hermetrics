package org.foxtrot.hermetrics.rules.builtin;

import org.foxtrot.hermetrics.canonical.value.CanonicalNumber;
import org.foxtrot.hermetrics.canonical.value.CanonicalValue;
import org.foxtrot.hermetrics.canonical.path.PathPattern;
import org.foxtrot.hermetrics.rules.EquivalenceRule;

import java.math.BigDecimal;

public record NumberToleranceRule(PathPattern path, BigDecimal epsilon) implements EquivalenceRule {

    @Override
    public boolean equivalent(CanonicalValue main, CanonicalValue load) {
        return main instanceof CanonicalNumber a
                && load instanceof CanonicalNumber b
                && a.value().subtract(b.value()).abs().compareTo(epsilon) <= 0;
    }
}
