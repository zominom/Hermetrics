package org.foxtrot.hermetrics.rules.builtin;

import org.foxtrot.hermetrics.canonical.value.CanonicalString;
import org.foxtrot.hermetrics.canonical.value.CanonicalValue;
import org.foxtrot.hermetrics.canonical.path.PathPattern;
import org.foxtrot.hermetrics.rules.TreeRewriteRule;

import java.util.Optional;

public record MaskRule(PathPattern path) implements TreeRewriteRule {

    public static final String TOKEN = "***";

    @Override
    public Optional<CanonicalValue> rewrite(CanonicalValue node) {
        return Optional.of(new CanonicalString(TOKEN));
    }
}
