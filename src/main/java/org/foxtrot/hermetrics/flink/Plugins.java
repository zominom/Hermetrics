package org.foxtrot.hermetrics.flink;

import org.foxtrot.hermetrics.config.ConfigLoader;
import org.foxtrot.hermetrics.decode.DecoderRegistry;
import org.foxtrot.hermetrics.diff.Differ;
import org.foxtrot.hermetrics.flink.sink.SinkRegistry;
import org.foxtrot.hermetrics.report.FindingCodec;
import org.foxtrot.hermetrics.rules.Normalizer;
import org.foxtrot.hermetrics.rules.RuleSetLoader;
import org.foxtrot.hermetrics.rules.RuleTypeRegistry;

import java.io.Serializable;

public interface Plugins extends Serializable {

    DecoderRegistry decoders();

    RuleTypeRegistry ruleTypes();

    SinkRegistry sinks();

    FindingCodec codec();

    Differ differ();

    Normalizer normalizer();

    default ConfigLoader configLoader() {
        return new ConfigLoader(new RuleSetLoader(ruleTypes()));
    }

    static Plugins defaults() {
        return new DefaultPlugins();
    }
}
