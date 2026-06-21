package org.foxtrot.hermetrics.flink.sink;

import org.apache.flink.api.common.serialization.SerializationSchema;
import org.apache.flink.api.connector.sink2.Sink;
import org.apache.flink.connector.base.DeliveryGuarantee;
import org.apache.flink.connector.kafka.sink.KafkaRecordSerializationSchema;
import org.apache.flink.connector.kafka.sink.KafkaSink;
import org.foxtrot.hermetrics.flink.record.KeyedRecord;
import org.foxtrot.hermetrics.flink.config.SinkConfig;

import java.nio.charset.StandardCharsets;
import java.util.Properties;
import java.util.Set;

public final class KafkaSinkFactory implements FindingSinkFactory {

    private static final Set<String> RESERVED_OPTIONS = Set.of("bootstrapServers", "topic");

    @Override
    public String typeId() {
        return "kafka";
    }

    @Override
    public Sink<KeyedRecord> create(SinkConfig config) {
        SerializationSchema<KeyedRecord> keySchema = record -> record.key().getBytes(StandardCharsets.UTF_8);
        SerializationSchema<KeyedRecord> valueSchema = record -> record.json().getBytes(StandardCharsets.UTF_8);
        return KafkaSink.<KeyedRecord>builder()
                .setBootstrapServers(config.require("bootstrapServers"))
                .setKafkaProducerConfig(producerConfig(config))
                .setRecordSerializer(KafkaRecordSerializationSchema.builder()
                        .setTopic(config.require("topic"))
                        .setKeySerializationSchema(new KeyedRecordKeySerializer())
                        .setValueSerializationSchema(new KeyedRecordValueSerializer())
                        .setValueSerializationSchema(valueSchema)
                        .build())
                .setDeliveryGuarantee(DeliveryGuarantee.AT_LEAST_ONCE)
                .build();
    }

    private static Properties producerConfig(SinkConfig config) {
        Properties properties = new Properties();
        config.options().forEach((key, value) -> {
            if (!RESERVED_OPTIONS.contains(key)) {
                properties.put(key, value);
            }
        });
        return properties;
    }

    static final class KeyedRecordKeySerializer implements SerializationSchema<KeyedRecord> {

        @Override
        public byte[] serialize(KeyedRecord element) {
            return element.key().getBytes(StandardCharsets.UTF_8);
        }
    }

    static final class KeyedRecordValueSerializer implements SerializationSchema<KeyedRecord> {

        @Override
        public byte[] serialize(KeyedRecord element) {
            return element.json().getBytes(StandardCharsets.UTF_8);
        }
    }
}
