package org.foxtrot.hermetrics.rules;

import org.foxtrot.hermetrics.canonical.value.CanonicalValue;

public interface EquivalenceRule extends NormalizationRule {

    boolean equivalent(CanonicalValue main, CanonicalValue load);
}
