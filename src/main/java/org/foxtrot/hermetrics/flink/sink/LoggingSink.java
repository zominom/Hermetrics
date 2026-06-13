package org.foxtrot.hermetrics.flink.sink;

import org.apache.flink.api.connector.sink2.Sink;
import org.apache.flink.api.connector.sink2.SinkWriter;
import org.apache.flink.api.connector.sink2.WriterInitContext;
import org.foxtrot.hermetrics.flink.record.KeyedRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class LoggingSink implements Sink<KeyedRecord> {

    @Override
    public SinkWriter<KeyedRecord> createWriter(WriterInitContext context) {
        return new Writer();
    }

    static final class Writer implements SinkWriter<KeyedRecord> {

        private static final Logger LOG = LoggerFactory.getLogger(LoggingSink.class);

        @Override
        public void write(KeyedRecord record, Context context) {
            LOG.info("{} {}", record.key(), record.json());
        }

        @Override
        public void flush(boolean endOfInput) {
        }

        @Override
        public void close() {
        }
    }
}
