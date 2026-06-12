package org.foxtrot.hermetrics.flink.operator;

import org.apache.flink.api.common.functions.AggregateFunction;
import org.foxtrot.hermetrics.flink.VerdictSummary;

public final class RollupAggregator implements AggregateFunction<VerdictSummary, RollupAccumulator, RollupAccumulator> {

    private static final int MAX_SAMPLE_GUIDS = 5;

    @Override
    public RollupAccumulator createAccumulator() {
        return new RollupAccumulator();
    }

    @Override
    public RollupAccumulator add(VerdictSummary summary, RollupAccumulator acc) {
        if (acc.count == 0) {
            acc.topic = summary.topic();
            acc.status = summary.status();
            acc.severity = summary.severity();
            acc.signatureId = summary.signatureId();
            acc.signatureText = summary.signatureText();
        }
        acc.count++;
        addSample(acc, summary.guid());
        return acc;
    }

    @Override
    public RollupAccumulator getResult(RollupAccumulator acc) {
        return acc;
    }

    @Override
    public RollupAccumulator merge(RollupAccumulator a, RollupAccumulator b) {
        RollupAccumulator base = a.count > 0 ? a : b;
        RollupAccumulator other = base == a ? b : a;
        base.count += other.count;
        other.sampleGuids.forEach(guid -> addSample(base, guid));
        return base;
    }

    private static void addSample(RollupAccumulator acc, String guid) {
        if (acc.sampleGuids.size() < MAX_SAMPLE_GUIDS && !acc.sampleGuids.contains(guid)) {
            acc.sampleGuids.add(guid);
        }
    }
}
