package org.foxtrot.hermetrics.match.state;

import java.io.Serializable;

public final class StateVersion implements Serializable {

    public String sequence;
    public String hash;
    public String stateJson;
    public long eventTimeMillis;

    public StateVersion() {
    }

    public StateVersion(String sequence, String hash, String stateJson, long eventTimeMillis) {
        this.sequence = sequence;
        this.hash = hash;
        this.stateJson = stateJson;
        this.eventTimeMillis = eventTimeMillis;
    }
}
