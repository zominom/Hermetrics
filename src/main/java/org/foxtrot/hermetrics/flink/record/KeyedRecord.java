package org.foxtrot.hermetrics.flink.record;

import java.io.Serializable;

public record KeyedRecord(String key, String json) implements Serializable {
}
