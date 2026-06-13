package org.foxtrot.hermetrics.decode.format;

import org.foxtrot.hermetrics.decode.DecodeException;
import org.foxtrot.hermetrics.decode.RawMessage;

import org.foxtrot.hermetrics.canonical.json.CanonicalJsonWriter;
import org.foxtrot.hermetrics.canonical.value.CanonicalNull;
import org.foxtrot.hermetrics.canonical.value.CanonicalNumber;
import org.foxtrot.hermetrics.canonical.value.CanonicalObject;
import org.foxtrot.hermetrics.canonical.value.CanonicalString;
import org.foxtrot.hermetrics.canonical.value.CanonicalValue;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JsonPayloadDecoderTest {

    private final JsonPayloadDecoder decoder = new JsonPayloadDecoder();

    @Test
    void decodesNestedStructure() {
        CanonicalValue tree = decoder.decode(RawMessage.of("t",
                "{\"z\": {\"items\": [1, \"two\", null, true]}, \"a\": -3.5}"));
        assertThat(CanonicalJsonWriter.write(tree))
                .isEqualTo("{\"a\":-3.5,\"z\":{\"items\":[1,\"two\",null,true]}}");
    }

    @Test
    void preservesDecimalPrecision() {
        CanonicalValue tree = decoder.decode(RawMessage.of("t", "{\"v\": 0.1}"));
        CanonicalNumber num = (CanonicalNumber) ((CanonicalObject) tree).fields().get("v");
        assertThat(num.value()).isEqualByComparingTo(new BigDecimal("0.1"));
    }

    @Test
    void decodesRootArrayAndScalars() {
        assertThat(CanonicalJsonWriter.write(decoder.decode(RawMessage.of("t", "[1, 2]")))).isEqualTo("[1,2]");
        assertThat(decoder.decode(RawMessage.of("t", "\"hello\""))).isEqualTo(new CanonicalString("hello"));
        assertThat(decoder.decode(RawMessage.of("t", "null"))).isEqualTo(CanonicalNull.INSTANCE);
    }

    @Test
    void invalidJsonThrows() {
        assertThatThrownBy(() -> decoder.decode(RawMessage.of("orders", "{not json")))
                .isInstanceOf(DecodeException.class)
                .hasMessageContaining("orders");
    }

    @Test
    void emptyPayloadThrows() {
        assertThatThrownBy(() -> decoder.decode(RawMessage.of("orders", "")))
                .isInstanceOf(DecodeException.class);
    }
}
