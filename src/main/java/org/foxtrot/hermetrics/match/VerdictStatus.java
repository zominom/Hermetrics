package org.foxtrot.hermetrics.match;

public enum VerdictStatus {
    EQUAL,
    EQUAL_DIVERGED,
    DIFF,
    MISSING_IN_LOAD,
    EXTRA_IN_LOAD,
    NOT_MIRRORED,
    TEST_TRAFFIC,
    UNANCHORED
}
