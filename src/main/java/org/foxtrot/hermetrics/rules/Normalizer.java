package org.foxtrot.hermetrics.rules;

import org.foxtrot.hermetrics.canonical.value.CanonicalArray;
import org.foxtrot.hermetrics.canonical.value.CanonicalNull;
import org.foxtrot.hermetrics.canonical.value.CanonicalObject;
import org.foxtrot.hermetrics.canonical.value.CanonicalValue;
import org.foxtrot.hermetrics.canonical.path.Path;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.TreeMap;

public final class Normalizer {

    public CanonicalValue normalize(CanonicalValue root, RuleSet rules) {
        CanonicalValue result = walk(Path.ROOT, root, rules);
        return result == null ? CanonicalNull.INSTANCE : result;
    }

    private CanonicalValue walk(Path path, CanonicalValue node, RuleSet rules) {
        node = applyRewrites(path, node, rules, false);
        if (node == null) {
            return null;
        }
        node = normalizeChildren(path, node, rules);
        return applyRewrites(path, node, rules, true);
    }

    private static CanonicalValue applyRewrites(Path path, CanonicalValue node, RuleSet rules, boolean postOrder) {
        for (TreeRewriteRule rule : rules.rewriteRules()) {
            if (rule.postOrder() == postOrder && rule.path().matches(path)) {
                Optional<CanonicalValue> rewritten = rule.rewrite(node);
                if (rewritten.isEmpty()) {
                    return null;
                }
                node = rewritten.get();
            }
        }
        return node;
    }

    private CanonicalValue normalizeChildren(Path path, CanonicalValue node, RuleSet rules) {
        return switch (node) {
            case CanonicalObject obj -> normalizeObject(path, obj, rules);
            case CanonicalArray arr -> normalizeArray(path, arr, rules);
            default -> node;
        };
    }

    private CanonicalValue normalizeObject(Path path, CanonicalObject obj, RuleSet rules) {
        TreeMap<String, CanonicalValue> out = new TreeMap<>();
        for (var entry : obj.fields().entrySet()) {
            CanonicalValue child = walk(path.child(entry.getKey()), entry.getValue(), rules);
            if (child == null || dropsAsNull(child, rules) || dropsAsEmpty(child, rules)) {
                continue;
            }
            out.put(entry.getKey(), child);
        }
        return new CanonicalObject(out);
    }

    private CanonicalValue normalizeArray(Path path, CanonicalArray arr, RuleSet rules) {
        List<CanonicalValue> out = new ArrayList<>(arr.items().size());
        int index = 0;
        for (CanonicalValue item : arr.items()) {
            CanonicalValue child = walk(path.index(index++), item, rules);
            if (child != null) {
                out.add(child);
            }
        }
        return new CanonicalArray(out);
    }

    private static boolean dropsAsNull(CanonicalValue child, RuleSet rules) {
        return rules.nullsEqualAbsent() && child instanceof CanonicalNull;
    }

    private static boolean dropsAsEmpty(CanonicalValue child, RuleSet rules) {
        if (!rules.emptyEqualsAbsent()) {
            return false;
        }
        return (child instanceof CanonicalObject obj && obj.fields().isEmpty())
                || (child instanceof CanonicalArray arr && arr.items().isEmpty());
    }
}
