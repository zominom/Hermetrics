package org.foxtrot.hermetrics.decode.format;

import org.foxtrot.hermetrics.decode.DecodeException;
import org.foxtrot.hermetrics.decode.PayloadDecoder;
import org.foxtrot.hermetrics.decode.RawMessage;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.foxtrot.hermetrics.canonical.json.CanonicalJsonReader;
import org.foxtrot.hermetrics.canonical.value.CanonicalValue;

import java.io.IOException;

public final class JsonPayloadDecoder implements PayloadDecoder {

    private final ObjectMapper mapper = JsonMapper.builder()
            .enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS)
            .build();

    @Override
    public String formatId() {
        return "json";
    }

    @Override
    public CanonicalValue decode(RawMessage message) {
        JsonNode root;
        try {
            root = mapper.readTree(message.value());
        } catch (IOException e) {
            throw new DecodeException("invalid JSON on topic " + message.topic(), e);
        }
        if (root == null || root.isMissingNode()) {
            throw new DecodeException("empty JSON payload on topic " + message.topic());
        }
        return CanonicalJsonReader.fromJackson(root);
    }
}
