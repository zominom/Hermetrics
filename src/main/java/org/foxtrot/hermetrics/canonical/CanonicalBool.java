package org.foxtrot.hermetrics.canonical;

public record CanonicalBool(boolean value) implements CanonicalValue {

    @Override
    public String typeName() {
        return "boolean";
    }
}
