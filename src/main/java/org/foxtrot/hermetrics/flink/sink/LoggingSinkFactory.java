package org.foxtrot.hermetrics.flink.sink;

import org.apache.flink.api.connector.sink2.Sink;
import org.foxtrot.hermetrics.flink.KeyedRecord;
import org.foxtrot.hermetrics.flink.config.SinkConfig;

public final class LoggingSinkFactory implements FindingSinkFactory {

    @Override
    public String typeId() {
        return "logging";
    }

    @Override
    public Sink<KeyedRecord> create(SinkConfig config) {
        return new LoggingSink();
    }
}
