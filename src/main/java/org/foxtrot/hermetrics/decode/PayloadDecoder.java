package org.foxtrot.hermetrics.decode;

import org.foxtrot.hermetrics.canonical.value.CanonicalValue;

public interface PayloadDecoder {

    String formatId();

    CanonicalValue decode(RawMessage message) throws DecodeException;
}
