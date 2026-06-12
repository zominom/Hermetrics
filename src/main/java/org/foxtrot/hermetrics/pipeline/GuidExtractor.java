package org.foxtrot.hermetrics.pipeline;

import org.foxtrot.hermetrics.canonical.CanonicalArray;
import org.foxtrot.hermetrics.canonical.CanonicalBool;
import org.foxtrot.hermetrics.canonical.CanonicalNumber;
import org.foxtrot.hermetrics.canonical.CanonicalObject;
import org.foxtrot.hermetrics.canonical.CanonicalString;
import org.foxtrot.hermetrics.canonical.CanonicalValue;
import org.foxtrot.hermetrics.canonical.Path;

import java.util.Optional;

public final class GuidExtractor {

    private GuidExtractor() {
    }

    public static Optional<String> extract(CanonicalValue root, Path guidPath) {
        CanonicalValue current = root;
        for (Path.Segment segment : guidPath.segments()) {
            current = step(current, segment);
            if (current == null) {
                return Optional.empty();
            }
        }
        return asText(current);
    }

    private static CanonicalValue step(CanonicalValue current, Path.Segment segment) {
        return switch (segment) {
            case Path.Segment.Field f ->
                    current instanceof CanonicalObject obj ? obj.fields().get(f.name()) : null;
            case Path.Segment.Index ix ->
                    current instanceof CanonicalArray arr
                            && ix.index() >= 0 && ix.index() < arr.items().size()
                            ? arr.items().get(ix.index()) : null;
        };
    }

    private static Optional<String> asText(CanonicalValue value) {
        return switch (value) {
            case CanonicalString s -> Optional.of(s.value());
            case CanonicalNumber n -> Optional.of(n.value().toPlainString());
            case CanonicalBool b -> Optional.of(String.valueOf(b.value()));
            default -> Optional.empty();
        };
    }
}
