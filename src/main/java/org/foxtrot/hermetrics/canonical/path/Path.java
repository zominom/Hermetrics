package org.foxtrot.hermetrics.canonical.path;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public record Path(List<Segment> segments) implements Serializable {

    public static final Path ROOT = new Path(List.of());

    public Path {
        segments = List.copyOf(segments);
    }

    public sealed interface Segment extends Serializable {
        record Field(String name) implements Segment {
        }

        record Index(int index) implements Segment {
        }
    }

    public static Path parse(String text) {
        PathPattern pattern = PathPattern.parse(text);
        List<Segment> segments = new ArrayList<>();
        for (PathPattern.Seg seg : pattern.segs()) {
            switch (seg) {
                case PathPattern.Seg.Field f -> segments.add(new Segment.Field(f.name()));
                case PathPattern.Seg.Index ix -> segments.add(new Segment.Index(ix.index()));
                default -> throw new IllegalArgumentException(
                        "wildcards are not allowed in a concrete path: " + text);
            }
        }
        return new Path(segments);
    }

    public Path child(String name) {
        List<Segment> next = new ArrayList<>(segments);
        next.add(new Segment.Field(name));
        return new Path(next);
    }

    public Path index(int i) {
        List<Segment> next = new ArrayList<>(segments);
        next.add(new Segment.Index(i));
        return new Path(next);
    }

    public boolean isRoot() {
        return segments.isEmpty();
    }

    public String render() {
        return render(false);
    }

    public String generalized() {
        return render(true);
    }

    private String render(boolean generalizeIndices) {
        if (segments.isEmpty()) {
            return "$";
        }
        StringBuilder sb = new StringBuilder();
        for (Segment segment : segments) {
            switch (segment) {
                case Segment.Field f -> {
                    if (!sb.isEmpty()) {
                        sb.append('.');
                    }
                    sb.append(f.name());
                }
                case Segment.Index ix -> {
                    sb.append('[');
                    if (!generalizeIndices) {
                        sb.append(ix.index());
                    }
                    sb.append(']');
                }
            }
        }
        return sb.toString();
    }

    @Override
    public String toString() {
        return render();
    }
}
