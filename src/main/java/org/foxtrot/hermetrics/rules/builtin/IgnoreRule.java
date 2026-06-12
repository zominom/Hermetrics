package org.foxtrot.hermetrics.rules.builtin;

import org.foxtrot.hermetrics.canonical.CanonicalValue;
import org.foxtrot.hermetrics.canonical.PathPattern;
import org.foxtrot.hermetrics.rules.TreeRewriteRule;

import java.util.Optional;

public record IgnoreRule(PathPattern path) implements TreeRewriteRule {

    @Override
    public Optional<CanonicalValue> rewrite(CanonicalValue node) {
        return Optional.empty();
    }
}
