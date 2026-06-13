package org.foxtrot.hermetrics.canonical.json;

import org.foxtrot.hermetrics.canonical.value.CanonicalArray;
import org.foxtrot.hermetrics.canonical.value.CanonicalBool;
import org.foxtrot.hermetrics.canonical.value.CanonicalNull;
import org.foxtrot.hermetrics.canonical.value.CanonicalNumber;
import org.foxtrot.hermetrics.canonical.value.CanonicalObject;
import org.foxtrot.hermetrics.canonical.value.CanonicalString;
import org.foxtrot.hermetrics.canonical.value.CanonicalValue;

public final class CanonicalJsonWriter {

    private CanonicalJsonWriter() {
    }

    public static String write(CanonicalValue value) {
        StringBuilder sb = new StringBuilder();
        append(value, sb);
        return sb.toString();
    }

    private static void append(CanonicalValue value, StringBuilder sb) {
        switch (value) {
            case CanonicalNull ignored -> sb.append("null");
            case CanonicalBool b -> sb.append(b.value());
            case CanonicalNumber n -> sb.append(n.value().toPlainString());
            case CanonicalString s -> appendString(s.value(), sb);
            case CanonicalArray arr -> appendArray(arr, sb);
            case CanonicalObject obj -> appendObject(obj, sb);
        }
    }

    private static void appendArray(CanonicalArray arr, StringBuilder sb) {
        sb.append('[');
        boolean first = true;
        for (CanonicalValue item : arr.items()) {
            if (!first) {
                sb.append(',');
            }
            first = false;
            append(item, sb);
        }
        sb.append(']');
    }

    private static void appendObject(CanonicalObject obj, StringBuilder sb) {
        sb.append('{');
        boolean first = true;
        for (var entry : obj.fields().entrySet()) {
            if (!first) {
                sb.append(',');
            }
            first = false;
            appendString(entry.getKey(), sb);
            sb.append(':');
            append(entry.getValue(), sb);
        }
        sb.append('}');
    }

    private static void appendString(String s, StringBuilder sb) {
        sb.append('"');
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
                }
            }
        }
        sb.append('"');
    }
}
