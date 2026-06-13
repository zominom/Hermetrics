package org.foxtrot.hermetrics.canonical.value;

public record CanonicalBool(boolean value) implements CanonicalValue {

    @Override
    public String typeName() {
        return "boolean";
    }
}
