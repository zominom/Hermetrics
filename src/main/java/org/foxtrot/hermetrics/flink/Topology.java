package org.foxtrot.hermetrics.flink;

import org.apache.flink.api.common.typeinfo.Types;
import org.apache.flink.streaming.api.datastream.BroadcastStream;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.windowing.assigners.TumblingProcessingTimeWindows;
import org.foxtrot.hermetrics.flink.config.JobConfig;
import org.foxtrot.hermetrics.flink.operator.DecideFunction;
import org.foxtrot.hermetrics.flink.operator.NormalizeFunction;
import org.foxtrot.hermetrics.flink.operator.RollupAggregator;
import org.foxtrot.hermetrics.flink.operator.RollupEmitter;
import org.foxtrot.hermetrics.match.Observation;

import java.time.Duration;

public final class Topology {

    private Topology() {
    }

    public static SingleOutputStreamOperator<KeyedRecord> build(JobConfig config,
                                                                DataStream<RawEnvRecord> records,
                                                                DataStream<String> control,
                                                                Plugins plugins) {
        BroadcastStream<String> broadcast = control.broadcast(ControlChannel.DESCRIPTOR);

        SingleOutputStreamOperator<Observation> observations = records
                .connect(broadcast)
                .process(new NormalizeFunction(config.compareJson(), plugins))
                .name("normalize").uid("normalize");

        observations.getSideOutput(NormalizeFunction.DEAD_LETTERS)
                .sinkTo(plugins.sinks().create(config.deadLetters()))
                .name("dead-letter-sink").uid("sink-dead-letters");

        SingleOutputStreamOperator<KeyedRecord> verdicts = observations
                .keyBy(Observation::guid, Types.STRING)
                .connect(broadcast)
                .process(new DecideFunction(config.compareJson(), config.stateTtlMillis(),
                        config.emitEqualVerdicts(), plugins))
                .name("decide").uid("decide");

        verdicts.sinkTo(plugins.sinks().create(config.results()))
                .name("results-sink").uid("sink-results");

        attachRollups(config, verdicts, plugins);
        return verdicts;
    }

    private static void attachRollups(JobConfig config, SingleOutputStreamOperator<KeyedRecord> verdicts,
                                      Plugins plugins) {
        if (config.rollups() == null) {
            return;
        }
        verdicts.getSideOutput(DecideFunction.SUMMARIES)
                .keyBy(VerdictSummary::rollupKey, Types.STRING)
                .window(TumblingProcessingTimeWindows.of(Duration.ofMillis(config.rollups().windowMillis())))
                .aggregate(new RollupAggregator(), new RollupEmitter(plugins.codec()))
                .name("rollups").uid("rollups")
                .sinkTo(plugins.sinks().create(config.rollups().sink()))
                .name("rollup-sink").uid("sink-rollups");
    }
}
