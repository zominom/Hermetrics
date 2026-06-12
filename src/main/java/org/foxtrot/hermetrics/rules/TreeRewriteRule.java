package org.foxtrot.hermetrics.rules;

import org.foxtrot.hermetrics.canonical.CanonicalValue;

import java.util.Optional;

public interface TreeRewriteRule extends NormalizationRule {

    Optional<CanonicalValue> rewrite(CanonicalValue node);

    default boolean postOrder() {
        return false;
    }
}
