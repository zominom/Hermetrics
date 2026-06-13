package org.foxtrot.hermetrics.flink.operator;

import org.apache.flink.api.common.functions.OpenContext;
import org.apache.flink.api.common.state.StateTtlConfig;
import org.apache.flink.api.common.state.ValueState;
import org.apache.flink.api.common.state.ValueStateDescriptor;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.common.typeinfo.Types;
import org.apache.flink.streaming.api.functions.co.KeyedBroadcastProcessFunction;
import org.apache.flink.util.Collector;
import org.apache.flink.util.OutputTag;
import org.foxtrot.hermetrics.config.CompareConfig;
import org.foxtrot.hermetrics.config.ConfigLoader;
import org.foxtrot.hermetrics.flink.ControlChannel;
import org.foxtrot.hermetrics.flink.record.KeyedRecord;
import org.foxtrot.hermetrics.flink.Plugins;
import org.foxtrot.hermetrics.flink.record.VerdictSummary;
import org.foxtrot.hermetrics.match.state.GuidState;
import org.foxtrot.hermetrics.match.MatchEngine;
import org.foxtrot.hermetrics.match.MatchPolicy;
import org.foxtrot.hermetrics.match.Observation;
import org.foxtrot.hermetrics.match.verdict.Verdict;
import org.foxtrot.hermetrics.match.verdict.VerdictStatus;
import org.foxtrot.hermetrics.report.FindingCodec;

import java.time.Duration;
import java.util.List;

public class DecideFunction extends KeyedBroadcastProcessFunction<String, Observation, String, KeyedRecord> {

    public static final OutputTag<VerdictSummary> SUMMARIES =
            new OutputTag<>("verdict-summaries", TypeInformation.of(VerdictSummary.class));

    private final String bootstrapCompareJson;
    private final long stateTtlMillis;
    private final boolean emitEqualVerdicts;
    private final Plugins plugins;

    private transient String activeJson;
    private transient MatchEngine engine;
    private transient ConfigLoader configLoader;
    private transient FindingCodec codec;
    private transient ValueState<GuidState> guidState;
    private transient ValueState<Long> nextFire;
    private transient ValueState<Long> firstSeenProcTime;

    public DecideFunction(String bootstrapCompareJson, long stateTtlMillis,
                          boolean emitEqualVerdicts, Plugins plugins) {
        this.bootstrapCompareJson = bootstrapCompareJson;
        this.stateTtlMillis = stateTtlMillis;
        this.emitEqualVerdicts = emitEqualVerdicts;
        this.plugins = plugins;
    }

    @Override
    public void open(OpenContext openContext) {
        StateTtlConfig ttl = StateTtlConfig.newBuilder(Duration.ofMillis(stateTtlMillis))
                .setUpdateType(StateTtlConfig.UpdateType.OnCreateAndWrite)
                .setStateVisibility(StateTtlConfig.StateVisibility.NeverReturnExpired)
                .build();
        guidState = state("guid-state", TypeInformation.of(GuidState.class), ttl);
        nextFire = state("next-fire", Types.LONG, ttl);
        firstSeenProcTime = state("first-seen-proc", Types.LONG, ttl);
        codec = plugins.codec();
    }

    @Override
    public void processElement(Observation observation, ReadOnlyContext ctx, Collector<KeyedRecord> out)
            throws Exception {
        MatchEngine engine = activeEngine(broadcastJson(ctx));
        GuidState state = stateOrNew();
        engine.onObservation(state, observation);
        guidState.update(state);
        scheduleDecision(ctx, engine.policy());
    }

    @Override
    public void onTimer(long timestamp, OnTimerContext ctx, Collector<KeyedRecord> out) throws Exception {
        if (isSuperseded(timestamp)) {
            return;
        }
        GuidState state = guidState.value();
        if (state == null) {
            return;
        }
        MatchEngine engine = activeEngine(broadcastJson(ctx));
        List<Verdict> verdicts = engine.decide(ctx.getCurrentKey(), state, System.currentTimeMillis());
        guidState.update(state);
        nextFire.clear();
        emit(verdicts, ctx, out);
    }

    @Override
    public void processBroadcastElement(String control, Context ctx, Collector<KeyedRecord> out) throws Exception {
        ControlChannel.apply(control, ctx.getBroadcastState(ControlChannel.DESCRIPTOR), configLoader());
    }

    private <T> ValueState<T> state(String name, TypeInformation<T> type, StateTtlConfig ttl) {
        ValueStateDescriptor<T> descriptor = new ValueStateDescriptor<>(name, type);
        descriptor.enableTimeToLive(ttl);
        return getRuntimeContext().getState(descriptor);
    }

    private static String broadcastJson(ReadOnlyContext ctx) throws Exception {
        return ctx.getBroadcastState(ControlChannel.DESCRIPTOR).get(ControlChannel.COMPARE_CONFIG_KEY);
    }

    private GuidState stateOrNew() throws Exception {
        GuidState state = guidState.value();
        return state == null ? new GuidState() : state;
    }

    private void scheduleDecision(ReadOnlyContext ctx, MatchPolicy policy) throws Exception {
        long now = ctx.timerService().currentProcessingTime();
        Long firstSeen = firstSeenProcTime.value();
        if (firstSeen == null) {
            firstSeen = now;
            firstSeenProcTime.update(now);
        }
        long fireAt = Math.min(now + policy.quietMillis(), firstSeen + policy.maxWaitMillis());
        if (fireAt <= now) {
            fireAt = now + 1;
        }
        ctx.timerService().registerProcessingTimeTimer(fireAt);
        nextFire.update(fireAt);
    }

    private boolean isSuperseded(long timestamp) throws Exception {
        Long expected = nextFire.value();
        return expected == null || timestamp != expected;
    }

    private void emit(List<Verdict> verdicts, OnTimerContext ctx, Collector<KeyedRecord> out) {
        for (Verdict verdict : verdicts) {
            ctx.output(SUMMARIES, VerdictSummary.of(verdict));
            if (!emitEqualVerdicts && verdict.status() == VerdictStatus.EQUAL) {
                continue;
            }
            out.collect(new KeyedRecord(verdict.guid() + "|" + verdict.topic(), codec.verdict(verdict)));
        }
    }

    private MatchEngine activeEngine(String broadcastJson) {
        String json = broadcastJson != null ? broadcastJson : bootstrapCompareJson;
        if (!json.equals(activeJson)) {
            CompareConfig config = configLoader().load(json);
            engine = new MatchEngine(config.policy(), config::ruleSetFor, plugins.differ());
            activeJson = json;
        }
        return engine;
    }

    private ConfigLoader configLoader() {
        if (configLoader == null) {
            configLoader = plugins.configLoader();
        }
        return configLoader;
    }
}
