package org.foxtrot.hermetrics.flink.operator;

import org.apache.flink.streaming.api.functions.windowing.ProcessWindowFunction;
import org.apache.flink.streaming.api.windowing.windows.TimeWindow;
import org.apache.flink.util.Collector;
import org.foxtrot.hermetrics.flink.record.KeyedRecord;
import org.foxtrot.hermetrics.report.FindingCodec;
import org.foxtrot.hermetrics.report.Rollup;

import java.util.List;

public final class RollupEmitter extends ProcessWindowFunction<RollupAccumulator, KeyedRecord, String, TimeWindow> {

    private final FindingCodec codec;

    public RollupEmitter(FindingCodec codec) {
        this.codec = codec;
    }

    @Override
    public void process(String key, Context context, Iterable<RollupAccumulator> elements,
                        Collector<KeyedRecord> out) {
        RollupAccumulator acc = elements.iterator().next();
        Rollup rollup = new Rollup(acc.topic, acc.status, acc.severity,
                acc.signatureId, signatureEntries(acc.signatureText),
                acc.count, acc.sampleGuids,
                context.window().getStart(), context.window().getEnd());
        out.collect(new KeyedRecord(key, codec.rollup(rollup)));
    }

    private static List<String> signatureEntries(String signatureText) {
        return signatureText.isEmpty() ? List.of() : List.of(signatureText.split("\n"));
    }
}
