package org.foxtrot.hermetrics.flink;

import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.connector.kafka.source.KafkaSource;
import org.apache.flink.connector.kafka.source.KafkaSourceBuilder;
import org.apache.flink.connector.kafka.source.enumerator.initializer.OffsetsInitializer;
import org.apache.flink.connector.kafka.source.reader.deserializer.KafkaRecordDeserializationSchema;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.foxtrot.hermetrics.flink.config.ClusterConfig;
import org.foxtrot.hermetrics.flink.config.ControlConfig;
import org.foxtrot.hermetrics.flink.config.EnvConfig;
import org.foxtrot.hermetrics.flink.config.JobConfig;
import org.foxtrot.hermetrics.match.Env;

import java.util.ArrayList;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

public final class KafkaSources {

    private KafkaSources() {
    }

    public static DataStream<RawEnvRecord> envRecords(StreamExecutionEnvironment env, JobConfig config) {
        DataStream<RawEnvRecord> merged = null;
        merged = appendClusters(env, merged, config.main(), Env.MAIN);
        merged = appendClusters(env, merged, config.load(), Env.LOAD);
        return merged;
    }

    public static DataStream<String> controlStream(StreamExecutionEnvironment env, JobConfig config) {
        return env
                .fromSource(controlSource(config.control()), WatermarkStrategy.noWatermarks(), "control-kafka")
                .uid("source-control");
    }

    private static DataStream<RawEnvRecord> appendClusters(StreamExecutionEnvironment env,
                                                           DataStream<RawEnvRecord> merged,
                                                           EnvConfig envConfig, Env side) {
        for (ClusterConfig cluster : envConfig.clusters()) {
            String sideName = side.name().toLowerCase(Locale.ROOT);
            DataStream<RawEnvRecord> stream = env
                    .fromSource(clusterSource(cluster, side), WatermarkStrategy.noWatermarks(),
                            sideName + "-kafka-" + cluster.name())
                    .uid("source-" + sideName + "-" + cluster.name());
            merged = merged == null ? stream : merged.union(stream);
        }
        return merged;
    }

    private static KafkaSource<RawEnvRecord> clusterSource(ClusterConfig cluster, Env side) {
        Map<String, String> physicalToLogical = cluster.physicalToLogical();
        KafkaSourceBuilder<RawEnvRecord> builder = KafkaSource.<RawEnvRecord>builder()
                .setBootstrapServers(cluster.bootstrapServers())
                .setTopics(new ArrayList<>(physicalToLogical.keySet()))
                .setGroupId(cluster.groupId())
                .setStartingOffsets(offsets(cluster.startingOffsets()))
                .setDeserializer(new EnvRecordDeserializer(side, physicalToLogical));
        if (!cluster.properties().isEmpty()) {
            builder.setProperties(properties(cluster.properties()));
        }
        return builder.build();
    }

    private static KafkaSource<String> controlSource(ControlConfig control) {
        KafkaSourceBuilder<String> builder = KafkaSource.<String>builder()
                .setBootstrapServers(control.bootstrapServers())
                .setTopics(control.topic())
                .setGroupId("hermetrics-control")
                .setStartingOffsets(OffsetsInitializer.earliest())
                .setDeserializer(KafkaRecordDeserializationSchema.valueOnly(StringDeserializer.class));
        if (!control.properties().isEmpty()) {
            builder.setProperties(properties(control.properties()));
        }
        return builder.build();
    }

    private static OffsetsInitializer offsets(String mode) {
        return "earliest".equals(mode) ? OffsetsInitializer.earliest() : OffsetsInitializer.latest();
    }

    private static Properties properties(Map<String, String> map) {
        Properties properties = new Properties();
        map.forEach(properties::put);
        return properties;
    }
}
