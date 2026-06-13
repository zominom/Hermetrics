package org.foxtrot.hermetrics.rules.builtin;

import org.foxtrot.hermetrics.canonical.value.CanonicalArray;
import org.foxtrot.hermetrics.canonical.json.CanonicalJsonWriter;
import org.foxtrot.hermetrics.canonical.value.CanonicalValue;
import org.foxtrot.hermetrics.canonical.path.PathPattern;
import org.foxtrot.hermetrics.rules.TreeRewriteRule;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public record UnorderedRule(PathPattern path) implements TreeRewriteRule {

    @Override
    public boolean postOrder() {
        return true;
    }

    @Override
    public Optional<CanonicalValue> rewrite(CanonicalValue node) {
        if (!(node instanceof CanonicalArray arr)) {
            return Optional.of(node);
        }
        List<CanonicalValue> sorted = new ArrayList<>(arr.items());
        sorted.sort(Comparator.comparing(CanonicalJsonWriter::write));
        return Optional.of(new CanonicalArray(sorted));
    }
}
