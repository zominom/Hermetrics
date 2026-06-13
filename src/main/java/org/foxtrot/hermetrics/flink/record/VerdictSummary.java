package org.foxtrot.hermetrics.flink.record;

import org.foxtrot.hermetrics.match.verdict.Verdict;

import java.io.Serializable;

public record VerdictSummary(String topic, String status, String severity,
                             String signatureId, String signatureText, String guid) implements Serializable {

    public static VerdictSummary of(Verdict verdict) {
        String signatureId = verdict.signature() == null ? "" : verdict.signature().id();
        String signatureText = verdict.signature() == null ? "" : String.join("\n", verdict.signature().entries());
        return new VerdictSummary(verdict.topic(), verdict.status().name(), verdict.severity().name(),
                signatureId, signatureText, verdict.guid());
    }

    public String rollupKey() {
        return topic + "|" + status + "|" + signatureId;
    }
}
