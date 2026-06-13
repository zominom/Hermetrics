package org.foxtrot.hermetrics.rules.builtin;

import org.foxtrot.hermetrics.canonical.value.CanonicalValue;
import org.foxtrot.hermetrics.canonical.path.PathPattern;
import org.foxtrot.hermetrics.rules.TreeRewriteRule;

import java.util.Optional;

public record IgnoreRule(PathPattern path) implements TreeRewriteRule {

    @Override
    public Optional<CanonicalValue> rewrite(CanonicalValue node) {
        return Optional.empty();
    }
}
