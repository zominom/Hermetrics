package org.foxtrot.hermetrics.testutil;

import org.foxtrot.hermetrics.canonical.CanonicalValue;
import org.foxtrot.hermetrics.decode.JsonPayloadDecoder;
import org.foxtrot.hermetrics.decode.RawMessage;

/** Builds canonical trees from JSON literals — keeps test fixtures readable. */
public final class Trees {

    private static final JsonPayloadDecoder JSON = new JsonPayloadDecoder();

    private Trees() {
    }

    public static CanonicalValue json(String json) {
        return JSON.decode(RawMessage.of("test", json));
    }
}
