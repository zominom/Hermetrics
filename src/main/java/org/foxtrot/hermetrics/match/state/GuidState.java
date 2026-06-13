package org.foxtrot.hermetrics.match.state;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public final class GuidState implements Serializable {

    public boolean enteredMain;
    public boolean enteredLoad;
    public long firstSeenMillis = -1;
    public long lastSeenMillis = -1;
    public int cohortRevision;
    public Map<String, TopicPair> topics = new HashMap<>();
}
