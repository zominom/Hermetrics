package org.foxtrot.hermetrics.report;

import java.util.List;

public record Rollup(String topic, String status, String severity,
                     String signatureId, List<String> signature,
                     long count, List<String> sampleGuids,
                     long windowStartMillis, long windowEndMillis) {

    public Rollup {
        signature = List.copyOf(signature);
        sampleGuids = List.copyOf(sampleGuids);
    }
}
