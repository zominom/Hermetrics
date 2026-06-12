package org.foxtrot.hermetrics.flink.operator;

import org.foxtrot.hermetrics.flink.VerdictSummary;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RollupAggregatorTest {

    private final RollupAggregator aggregator = new RollupAggregator();

    private static VerdictSummary summary(String guid) {
        return new VerdictSummary("orders", "DIFF", "ERROR", "sig1", "total: value-changed", guid);
    }

    @Test
    void aggregatesCountsAndSampleGuids() {
        RollupAccumulator acc = aggregator.createAccumulator();
        acc = aggregator.add(summary("g1"), acc);
        acc = aggregator.add(summary("g2"), acc);
        acc = aggregator.add(summary("g1"), acc);

        assertThat(acc.count).isEqualTo(3);
        assertThat(acc.sampleGuids).containsExactly("g1", "g2");
        assertThat(acc.topic).isEqualTo("orders");
        assertThat(acc.signatureId).isEqualTo("sig1");
    }

    @Test
    void samplesAreCappedAtFive() {
        RollupAccumulator acc = aggregator.createAccumulator();
        for (int i = 0; i < 10; i++) {
            acc = aggregator.add(summary("g" + i), acc);
        }
        assertThat(acc.count).isEqualTo(10);
        assertThat(acc.sampleGuids).hasSize(5);
    }

    @Test
    void mergeCombinesPartialAccumulators() {
        RollupAccumulator a = aggregator.add(summary("g1"), aggregator.createAccumulator());
        RollupAccumulator b = aggregator.add(summary("g2"), aggregator.createAccumulator());
        RollupAccumulator merged = aggregator.merge(a, b);
        assertThat(merged.count).isEqualTo(2);
        assertThat(merged.sampleGuids).containsExactlyInAnyOrder("g1", "g2");
    }
}
