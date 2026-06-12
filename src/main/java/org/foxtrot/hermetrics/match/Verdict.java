package org.foxtrot.hermetrics.match;

import org.foxtrot.hermetrics.diff.DiffSignature;
import org.foxtrot.hermetrics.diff.FieldDiff;

import java.io.Serializable;
import java.util.List;

public record Verdict(String guid, String topic, VerdictStatus status, Severity severity,
                      DiffSignature signature, List<FieldDiff> fieldDiffs, VerdictStats stats,
                      int revision, long decidedAtMillis) implements Serializable {

    public static final String COHORT_TOPIC = "(cohort)";

    public Verdict {
        fieldDiffs = fieldDiffs == null ? List.of() : List.copyOf(fieldDiffs);
    }
}
