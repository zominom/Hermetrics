package org.foxtrot.hermetrics.match;

import java.io.Serializable;

public record MatchPolicy(long quietMillis, long maxWaitMillis, boolean strictIntermediates,
                          CohortMode cohortMode) implements Serializable {

    public enum CohortMode {
        ENTRY_TOPICS, ASSUME_ALL
    }

    public static MatchPolicy defaults() {
        return new MatchPolicy(300_000L, 3_600_000L, false, CohortMode.ENTRY_TOPICS);
    }
}
