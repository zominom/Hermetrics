package org.foxtrot.hermetrics.flink;

import java.io.Serializable;

public record KeyedRecord(String key, String json) implements Serializable {
}
