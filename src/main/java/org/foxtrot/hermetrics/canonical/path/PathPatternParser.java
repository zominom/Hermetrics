package org.foxtrot.hermetrics.canonical.path;

import java.util.ArrayList;
import java.util.List;

final class PathPatternParser {

    private PathPatternParser() {
    }

    static List<PathPattern.Seg> parse(String text) {
        String t = stripRootPrefix(text.trim());
        if (t == null) {
            return List.of();
        }
        if (t.isEmpty()) {
            throw new IllegalArgumentException("empty path pattern: '" + text + "'");
        }
        return scan(t, text);
    }

    private static String stripRootPrefix(String t) {
        if (t.equals("$")) {
            return null;
        }
        if (t.startsWith("$.")) {
            return t.substring(2);
        }
        if (t.startsWith("$[")) {
            return t.substring(1);
        }
        return t;
    }

    private static List<PathPattern.Seg> scan(String t, String original) {
        List<PathPattern.Seg> segs = new ArrayList<>();
        StringBuilder name = new StringBuilder();
        boolean justClosedBracket = false;
        for (int i = 0; i < t.length(); i++) {
            char c = t.charAt(i);
            switch (c) {
                case '\\' -> {
                    if (i + 1 >= t.length()) {
                        throw new IllegalArgumentException("dangling escape in pattern: " + original);
                    }
                    name.append(t.charAt(++i));
                    justClosedBracket = false;
                }
                case '.' -> {
                    if (name.isEmpty() && !justClosedBracket) {
                        throw new IllegalArgumentException("empty segment in pattern: " + original);
                    }
                    flushName(name, segs);
                    justClosedBracket = false;
                }
                case '[' -> {
                    flushName(name, segs);
                    i = scanBracket(t, i, segs, original);
                    justClosedBracket = true;
                }
                default -> {
                    name.append(c);
                    justClosedBracket = false;
                }
            }
        }
        if (name.isEmpty() && !justClosedBracket) {
            throw new IllegalArgumentException("pattern ends with an empty segment: " + original);
        }
        flushName(name, segs);
        return segs;
    }

    private static int scanBracket(String t, int openIndex, List<PathPattern.Seg> segs, String original) {
        int close = t.indexOf(']', openIndex);
        if (close < 0) {
            throw new IllegalArgumentException("unclosed '[' in pattern: " + original);
        }
        String inside = t.substring(openIndex + 1, close);
        if (inside.isEmpty() || inside.equals("*")) {
            segs.add(new PathPattern.Seg.AnyIndex());
        } else {
            try {
                segs.add(new PathPattern.Seg.Index(Integer.parseInt(inside)));
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("invalid array index '" + inside + "' in pattern: " + original);
            }
        }
        return close;
    }

    private static void flushName(StringBuilder name, List<PathPattern.Seg> segs) {
        if (name.isEmpty()) {
            return;
        }
        String s = name.toString();
        name.setLength(0);
        switch (s) {
            case "**" -> segs.add(new PathPattern.Seg.Deep());
            case "*" -> segs.add(new PathPattern.Seg.AnyField());
            default -> segs.add(new PathPattern.Seg.Field(s));
        }
    }
}
