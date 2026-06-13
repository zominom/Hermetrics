package org.foxtrot.hermetrics.flink.source;

import org.foxtrot.hermetrics.flink.record.RawEnvRecord;

import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.connector.kafka.source.reader.deserializer.KafkaRecordDeserializationSchema;
import org.apache.flink.util.Collector;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.foxtrot.hermetrics.match.Env;

import java.util.HashMap;
import java.util.Map;

public final class EnvRecordDeserializer implements KafkaRecordDeserializationSchema<RawEnvRecord> {

    private final Env env;
    private final HashMap<String, String> physicalToLogical;

    public EnvRecordDeserializer(Env env, Map<String, String> physicalToLogical) {
        this.env = env;
        this.physicalToLogical = new HashMap<>(physicalToLogical);
    }

    @Override
    public void deserialize(ConsumerRecord<byte[], byte[]> record, Collector<RawEnvRecord> out) {
        String logicalTopic = physicalToLogical.getOrDefault(record.topic(), record.topic());
        out.collect(new RawEnvRecord(env, logicalTopic, record.key(), record.value(), record.timestamp()));
    }

    @Override
    public TypeInformation<RawEnvRecord> getProducedType() {
        return TypeInformation.of(RawEnvRecord.class);
    }
}
