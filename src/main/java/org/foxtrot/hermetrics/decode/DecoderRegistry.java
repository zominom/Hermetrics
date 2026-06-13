package org.foxtrot.hermetrics.decode;

import org.foxtrot.hermetrics.decode.format.JsonPayloadDecoder;
import org.foxtrot.hermetrics.decode.format.XmlPayloadDecoder;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public final class DecoderRegistry {

    private final Map<String, PayloadDecoder> byFormat = new HashMap<>();

    public static DecoderRegistry withDefaults() {
        return new DecoderRegistry()
                .register(new JsonPayloadDecoder())
                .register(new XmlPayloadDecoder());
    }

    public DecoderRegistry register(PayloadDecoder decoder) {
        byFormat.put(decoder.formatId().toLowerCase(Locale.ROOT), decoder);
        return this;
    }

    public PayloadDecoder forFormat(String formatId) {
        PayloadDecoder decoder = byFormat.get(formatId.toLowerCase(Locale.ROOT));
        if (decoder == null) {
            throw new IllegalArgumentException(
                    "no decoder registered for format '" + formatId + "', registered: " + byFormat.keySet());
        }
        return decoder;
    }
}
