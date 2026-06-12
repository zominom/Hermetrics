package org.foxtrot.hermetrics.flink.sink;

import org.apache.flink.api.connector.sink2.Sink;
import org.foxtrot.hermetrics.flink.KeyedRecord;
import org.foxtrot.hermetrics.flink.config.SinkConfig;

public interface FindingSinkFactory {

    String typeId();

    Sink<KeyedRecord> create(SinkConfig config);
}
