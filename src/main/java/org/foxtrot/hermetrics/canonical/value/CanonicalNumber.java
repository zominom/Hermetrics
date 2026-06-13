package org.foxtrot.hermetrics.canonical.value;

import java.math.BigDecimal;

public record CanonicalNumber(BigDecimal value) implements CanonicalValue {

    public CanonicalNumber {
        value = value.stripTrailingZeros();
    }

    public static CanonicalNumber of(long value) {
        return new CanonicalNumber(BigDecimal.valueOf(value));
    }

    public static CanonicalNumber of(String value) {
        return new CanonicalNumber(new BigDecimal(value));
    }

    @Override
    public String typeName() {
        return "number";
    }
}
