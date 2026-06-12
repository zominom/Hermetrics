package org.foxtrot.hermetrics.canonical;

public record CanonicalString(String value) implements CanonicalValue {

    @Override
    public String typeName() {
        return "string";
    }
}
