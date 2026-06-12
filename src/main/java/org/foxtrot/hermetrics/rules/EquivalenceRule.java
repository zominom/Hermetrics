package org.foxtrot.hermetrics.rules;

import org.foxtrot.hermetrics.canonical.CanonicalValue;

public interface EquivalenceRule extends NormalizationRule {

    boolean equivalent(CanonicalValue main, CanonicalValue load);
}
