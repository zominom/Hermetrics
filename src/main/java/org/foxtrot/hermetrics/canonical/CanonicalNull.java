package org.foxtrot.hermetrics.canonical;

public record CanonicalNull() implements CanonicalValue {

    public static final CanonicalNull INSTANCE = new CanonicalNull();

    @Override
    public String typeName() {
        return "null";
    }
}
