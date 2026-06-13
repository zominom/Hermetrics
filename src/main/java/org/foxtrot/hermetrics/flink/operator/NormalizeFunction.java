package org.foxtrot.hermetrics.flink.operator;

import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.streaming.api.functions.co.BroadcastProcessFunction;
import org.apache.flink.util.Collector;
import org.apache.flink.util.OutputTag;
import org.foxtrot.hermetrics.config.CompareConfig;
import org.foxtrot.hermetrics.config.ConfigLoader;
import org.foxtrot.hermetrics.decode.DecodeException;
import org.foxtrot.hermetrics.decode.RawMessage;
import org.foxtrot.hermetrics.flink.ControlChannel;
import org.foxtrot.hermetrics.flink.record.KeyedRecord;
import org.foxtrot.hermetrics.flink.Plugins;
import org.foxtrot.hermetrics.flink.record.RawEnvRecord;
import org.foxtrot.hermetrics.match.Observation;
import org.foxtrot.hermetrics.pipeline.ObservationFactory;
import org.foxtrot.hermetrics.report.DeadLetter;
import org.foxtrot.hermetrics.report.FindingCodec;

import java.util.Map;

public class NormalizeFunction extends BroadcastProcessFunction<RawEnvRecord, String, Observation> {

    public static final OutputTag<KeyedRecord> DEAD_LETTERS =
            new OutputTag<>("dead-letters", TypeInformation.of(KeyedRecord.class));

    private final String bootstrapCompareJson;
    private final Plugins plugins;

    private transient String activeJson;
    private transient ObservationFactory factory;
    private transient ConfigLoader configLoader;
    private transient FindingCodec codec;

    public NormalizeFunction(String bootstrapCompareJson, Plugins plugins) {
        this.bootstrapCompareJson = bootstrapCompareJson;
        this.plugins = plugins;
    }

    @Override
    public void processElement(RawEnvRecord record, ReadOnlyContext ctx, Collector<Observation> out)
            throws Exception {
        ObservationFactory factory = activeFactory(
                ctx.getBroadcastState(ControlChannel.DESCRIPTOR).get(ControlChannel.COMPARE_CONFIG_KEY));
        RawMessage message = new RawMessage(
                record.topic(), record.key(), record.value(), Map.of(), record.timestampMillis());
        try {
            for (Observation observation : factory.observe(message, record.env())) {
                out.collect(observation);
            }
        } catch (DecodeException | IllegalArgumentException e) {
            ctx.output(DEAD_LETTERS, deadLetter(record, e));
        }
    }

    @Override
    public void processBroadcastElement(String control, Context ctx, Collector<Observation> out) throws Exception {
        ControlChannel.apply(control, ctx.getBroadcastState(ControlChannel.DESCRIPTOR), configLoader());
    }

    private ObservationFactory activeFactory(String broadcastJson) {
        String json = broadcastJson != null ? broadcastJson : bootstrapCompareJson;
        if (!json.equals(activeJson)) {
            CompareConfig config = configLoader().load(json);
            factory = new ObservationFactory(config, plugins.decoders(), plugins.normalizer());
            activeJson = json;
        }
        return factory;
    }

    private ConfigLoader configLoader() {
        if (configLoader == null) {
            configLoader = plugins.configLoader();
        }
        return configLoader;
    }

    private KeyedRecord deadLetter(RawEnvRecord record, RuntimeException error) {
        if (codec == null) {
            codec = plugins.codec();
        }
        return new KeyedRecord(
                record.env() + "|" + record.topic(),
                codec.deadLetter(new DeadLetter(record.env().name(), record.topic(),
                        record.timestampMillis(), error.getMessage(), record.value())));
    }
}
