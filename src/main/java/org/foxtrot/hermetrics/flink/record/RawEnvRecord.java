package org.foxtrot.hermetrics.flink.record;

import org.foxtrot.hermetrics.match.Env;

import java.io.Serializable;

public record RawEnvRecord(Env env, String topic, byte[] key, byte[] value,
                           long timestampMillis) implements Serializable {
}
