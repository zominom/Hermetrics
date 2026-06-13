package org.foxtrot.hermetrics.canonical.value;

public record CanonicalString(String value) implements CanonicalValue {

    @Override
    public String typeName() {
        return "string";
    }
}
