package org.foxtrot.hermetrics.canonical.value;

import java.util.List;

public record CanonicalArray(List<CanonicalValue> items) implements CanonicalValue {

    public CanonicalArray {
        items = List.copyOf(items);
    }

    @Override
    public String typeName() {
        return "array";
    }
}
