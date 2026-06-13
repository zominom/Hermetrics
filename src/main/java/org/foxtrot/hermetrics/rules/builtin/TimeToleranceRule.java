package org.foxtrot.hermetrics.rules.builtin;

import org.foxtrot.hermetrics.canonical.value.CanonicalNumber;
import org.foxtrot.hermetrics.canonical.value.CanonicalString;
import org.foxtrot.hermetrics.canonical.value.CanonicalValue;
import org.foxtrot.hermetrics.canonical.path.PathPattern;
import org.foxtrot.hermetrics.rules.EquivalenceRule;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;

public record TimeToleranceRule(PathPattern path, long toleranceMillis, EpochUnit epochUnit)
        implements EquivalenceRule {

    public enum EpochUnit {MILLIS, SECONDS}

    @Override
    public boolean equivalent(CanonicalValue main, CanonicalValue load) {
        Instant a = parseInstant(main);
        Instant b = parseInstant(load);
        return a != null && b != null && Math.abs(a.toEpochMilli() - b.toEpochMilli()) <= toleranceMillis;
    }

    private Instant parseInstant(CanonicalValue value) {
        if (value instanceof CanonicalString s) {
            return parseInstant(s.value());
        }
        if (value instanceof CanonicalNumber n) {
            long epoch = n.value().longValue();
            return epochUnit == EpochUnit.SECONDS ? Instant.ofEpochSecond(epoch) : Instant.ofEpochMilli(epoch);
        }
        return null;
    }

    private static Instant parseInstant(String text) {
        try {
            return Instant.parse(text);
        } catch (DateTimeParseException e) {
            try {
                return OffsetDateTime.parse(text).toInstant();
            } catch (DateTimeParseException e2) {
                return null;
            }
        }
    }
}
