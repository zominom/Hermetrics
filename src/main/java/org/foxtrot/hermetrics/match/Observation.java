package org.foxtrot.hermetrics.match;

import org.foxtrot.hermetrics.canonical.CanonicalJsonWriter;
import org.foxtrot.hermetrics.canonical.CanonicalValue;
import org.foxtrot.hermetrics.canonical.ContentHasher;

import java.io.Serializable;

public record Observation(Env env, String topic, String guid, Kind kind,
                          String stateHash, String stateJson, long eventTimeMillis) implements Serializable {

    public enum Kind {
        ENTRY, OUTPUT
    }

    public static Observation entry(Env env, String topic, String guid, long eventTimeMillis) {
        return new Observation(env, topic, guid, Kind.ENTRY, null, null, eventTimeMillis);
    }

    public static Observation output(Env env, String topic, String guid,
                                     CanonicalValue normalizedState, long eventTimeMillis) {
        String stateJson = CanonicalJsonWriter.write(normalizedState);
        return new Observation(env, topic, guid, Kind.OUTPUT,
                ContentHasher.hash(stateJson), stateJson, eventTimeMillis);
    }
}
