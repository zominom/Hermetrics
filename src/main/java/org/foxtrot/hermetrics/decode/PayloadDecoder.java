package org.foxtrot.hermetrics.decode;

import org.foxtrot.hermetrics.canonical.CanonicalValue;

public interface PayloadDecoder {

    String formatId();

    CanonicalValue decode(RawMessage message) throws DecodeException;
}
