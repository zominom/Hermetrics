package org.foxtrot.hermetrics.rules.builtin;

import org.foxtrot.hermetrics.canonical.CanonicalString;
import org.foxtrot.hermetrics.canonical.CanonicalValue;
import org.foxtrot.hermetrics.canonical.PathPattern;
import org.foxtrot.hermetrics.rules.TreeRewriteRule;

import java.util.Optional;

public record MaskRule(PathPattern path) implements TreeRewriteRule {

    public static final String TOKEN = "***";

    @Override
    public Optional<CanonicalValue> rewrite(CanonicalValue node) {
        return Optional.of(new CanonicalString(TOKEN));
    }
}
