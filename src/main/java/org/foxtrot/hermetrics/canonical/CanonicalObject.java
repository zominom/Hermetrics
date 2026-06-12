package org.foxtrot.hermetrics.canonical;

import java.util.Collections;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

public record CanonicalObject(SortedMap<String, CanonicalValue> fields) implements CanonicalValue {

    public CanonicalObject {
        fields = Collections.unmodifiableSortedMap(new TreeMap<>(fields));
    }

    public static CanonicalObject of(Map<String, CanonicalValue> fields) {
        return new CanonicalObject(new TreeMap<>(fields));
    }

    @Override
    public String typeName() {
        return "object";
    }
}
