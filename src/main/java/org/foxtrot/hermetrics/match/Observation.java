package org.foxtrot.hermetrics.match;

import org.foxtrot.hermetrics.canonical.CanonicalJsonWriter;
import org.foxtrot.hermetrics.canonical.CanonicalValue;
import org.foxtrot.hermetrics.canonical.ContentHasher;

import java.io.Serializable;

public record Observation(Env env, String topic, String guid, String sequence, Kind kind,
                          String stateHash, String stateJson, long eventTimeMillis) implements Serializable {

    public enum Kind {
        ENTRY, OUTPUT
    }

    public static Observation entry(Env env, String topic, String guid, long eventTimeMillis) {
        return new Observation(env, topic, guid, null, Kind.ENTRY, null, null, eventTimeMillis);
    }

    public static Observation output(Env env, String topic, String guid,
                                     CanonicalValue normalizedState, long eventTimeMillis) {
        return output(env, topic, guid, null, normalizedState, eventTimeMillis);
    }

    public static Observation output(Env env, String topic, String guid, String sequence,
                                     CanonicalValue normalizedState, long eventTimeMillis) {
        String stateJson = CanonicalJsonWriter.write(normalizedState);
        return new Observation(env, topic, guid, sequence, Kind.OUTPUT,
                ContentHasher.hash(stateJson), stateJson, eventTimeMillis);
    }
}
