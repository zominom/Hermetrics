package org.foxtrot.hermetrics.rules.builtin;

import org.foxtrot.hermetrics.canonical.path.PathPattern;
import org.foxtrot.hermetrics.canonical.value.CanonicalBool;
import org.foxtrot.hermetrics.canonical.value.CanonicalNumber;
import org.foxtrot.hermetrics.canonical.value.CanonicalString;
import org.foxtrot.hermetrics.canonical.value.CanonicalValue;
import org.foxtrot.hermetrics.rules.TreeRewriteRule;

import java.util.Optional;

public record CastRule(PathPattern path, TargetType to) implements TreeRewriteRule {

    public enum TargetType {
        NUMBER,
        BOOLEAN
    }

    @Override
    public Optional<CanonicalValue> rewrite(CanonicalValue node) {
        if (!(node instanceof CanonicalString text)) {
            return Optional.of(node);
        }
        CanonicalValue cast = switch (to) {
            case NUMBER -> toNumber(text.value());
            case BOOLEAN -> toBoolean(text.value());
        };
        return Optional.of(cast == null ? node : cast);
    }

    private static CanonicalValue toNumber(String raw) {
        try {
            return CanonicalNumber.of(raw.trim());
        } catch (NumberFormatException notANumber) {
            return null;
        }
    }

    private static CanonicalValue toBoolean(String raw) {
        String value = raw.trim();
        if (value.equalsIgnoreCase("true")) {
            return new CanonicalBool(true);
        }
        if (value.equalsIgnoreCase("false")) {
            return new CanonicalBool(false);
        }
        return null;
    }
}
