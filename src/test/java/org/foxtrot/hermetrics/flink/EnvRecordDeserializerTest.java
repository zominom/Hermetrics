package org.foxtrot.hermetrics.flink;

import org.apache.flink.api.common.functions.util.ListCollector;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.apache.kafka.common.record.TimestampType;
import org.foxtrot.hermetrics.match.Env;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class EnvRecordDeserializerTest {

    private static ConsumerRecord<byte[], byte[]> record(String topic, long timestamp, String value) {
        return new ConsumerRecord<>(topic, 0, 5L, timestamp, TimestampType.CREATE_TIME,
                -1, -1, null, value.getBytes(StandardCharsets.UTF_8), new RecordHeaders(), Optional.empty());
    }

    @Test
    void mapsPhysicalTopicsBackToLogicalNames() throws Exception {
        EnvRecordDeserializer deserializer =
                new EnvRecordDeserializer(Env.LOAD, Map.of("load.orders", "orders"));
        List<RawEnvRecord> out = new ArrayList<>();
        deserializer.deserialize(record("load.orders", 42L, "{}"), new ListCollector<>(out));

        assertThat(out).hasSize(1);
        RawEnvRecord result = out.get(0);
        assertThat(result.env()).isEqualTo(Env.LOAD);
        assertThat(result.topic()).isEqualTo("orders");
        assertThat(result.timestampMillis()).isEqualTo(42L);
        assertThat(new String(result.value(), StandardCharsets.UTF_8)).isEqualTo("{}");
    }

    @Test
    void unknownTopicsPassThroughUnchanged() throws Exception {
        EnvRecordDeserializer deserializer = new EnvRecordDeserializer(Env.MAIN, Map.of());
        List<RawEnvRecord> out = new ArrayList<>();
        deserializer.deserialize(record("anything", 1L, "{}"), new ListCollector<>(out));
        assertThat(out.get(0).topic()).isEqualTo("anything");
        assertThat(out.get(0).env()).isEqualTo(Env.MAIN);
    }
}
