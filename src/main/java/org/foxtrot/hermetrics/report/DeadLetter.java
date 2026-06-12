package org.foxtrot.hermetrics.report;

public record DeadLetter(String env, String topic, long timestampMillis, String error, byte[] payload) {
}
