package org.foxtrot.hermetrics.flink;

import org.foxtrot.hermetrics.decode.DecoderRegistry;
import org.foxtrot.hermetrics.diff.Differ;
import org.foxtrot.hermetrics.diff.StructuralDiffer;
import org.foxtrot.hermetrics.flink.sink.SinkRegistry;
import org.foxtrot.hermetrics.report.FindingCodec;
import org.foxtrot.hermetrics.report.JsonFindingCodec;
import org.foxtrot.hermetrics.rules.Normalizer;
import org.foxtrot.hermetrics.rules.RuleTypeRegistry;

public class DefaultPlugins implements Plugins {

    @Override
    public DecoderRegistry decoders() {
        return DecoderRegistry.withDefaults();
    }

    @Override
    public RuleTypeRegistry ruleTypes() {
        return RuleTypeRegistry.withDefaults();
    }

    @Override
    public SinkRegistry sinks() {
        return SinkRegistry.withDefaults();
    }

    @Override
    public FindingCodec codec() {
        return new JsonFindingCodec();
    }

    @Override
    public Differ differ() {
        return new StructuralDiffer();
    }

    @Override
    public Normalizer normalizer() {
        return new Normalizer();
    }
}
