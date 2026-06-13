package org.foxtrot.hermetrics.canonical.path;

import java.io.Serializable;
import java.util.List;
import java.util.Objects;

public final class PathPattern implements Serializable {

    private final String text;
    private final List<Seg> segs;

    private PathPattern(String text, List<Seg> segs) {
        this.text = text;
        this.segs = List.copyOf(segs);
    }

    public sealed interface Seg extends Serializable {
        record Field(String name) implements Seg {
        }

        record AnyField() implements Seg {
        }

        record Index(int index) implements Seg {
        }

        record AnyIndex() implements Seg {
        }

        record Deep() implements Seg {
        }
    }

    public static PathPattern parse(String text) {
        Objects.requireNonNull(text, "pattern");
        return new PathPattern(text, PathPatternParser.parse(text));
    }

    public String text() {
        return text;
    }

    public boolean matches(Path path) {
        return match(0, path.segments(), 0);
    }

    List<Seg> segs() {
        return segs;
    }

    private boolean match(int pi, List<Path.Segment> s, int si) {
        if (pi == segs.size()) {
            return si == s.size();
        }
        Seg p = segs.get(pi);
        if (p instanceof Seg.Deep) {
            for (int k = si; k <= s.size(); k++) {
                if (match(pi + 1, s, k)) {
                    return true;
                }
            }
            return false;
        }
        if (si == s.size()) {
            return false;
        }
        return matchesSingle(p, s.get(si)) && match(pi + 1, s, si + 1);
    }

    private static boolean matchesSingle(Seg pattern, Path.Segment segment) {
        return switch (pattern) {
            case Seg.Field f -> segment instanceof Path.Segment.Field sf && sf.name().equals(f.name());
            case Seg.AnyField ignored -> segment instanceof Path.Segment.Field;
            case Seg.Index ix -> segment instanceof Path.Segment.Index si && si.index() == ix.index();
            case Seg.AnyIndex ignored -> segment instanceof Path.Segment.Index;
            case Seg.Deep ignored -> false;
        };
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof PathPattern other && segs.equals(other.segs);
    }

    @Override
    public int hashCode() {
        return segs.hashCode();
    }

    @Override
    public String toString() {
        return text;
    }
}
