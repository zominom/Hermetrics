package org.foxtrot.hermetrics.api;

public record FindingsQuery(String topic, int limit) {

    public boolean hasTopic() {
        return topic != null && !topic.isBlank();
    }
}
