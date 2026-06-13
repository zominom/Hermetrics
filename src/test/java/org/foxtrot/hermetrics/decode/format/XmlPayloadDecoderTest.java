package org.foxtrot.hermetrics.decode.format;

import org.foxtrot.hermetrics.decode.DecodeException;
import org.foxtrot.hermetrics.decode.RawMessage;

import org.foxtrot.hermetrics.canonical.json.CanonicalJsonWriter;
import org.foxtrot.hermetrics.canonical.value.CanonicalObject;
import org.foxtrot.hermetrics.canonical.value.CanonicalString;
import org.foxtrot.hermetrics.canonical.value.CanonicalValue;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class XmlPayloadDecoderTest {

    private final XmlPayloadDecoder decoder = new XmlPayloadDecoder();

    private String decodeToString(String xml) {
        CanonicalValue tree = decoder.decode(RawMessage.of("t", xml));
        return CanonicalJsonWriter.write(tree);
    }

    @Test
    void simpleElementsKeepRootName() {
        assertThat(decodeToString("<order><id>42</id></order>"))
                .isEqualTo("{\"order\":{\"id\":\"42\"}}");
    }

    @Test
    void attributesAndMixedTextGetMarkers() {
        assertThat(decodeToString("<order id=\"42\">total</order>"))
                .isEqualTo("{\"order\":{\"#text\":\"total\",\"@id\":\"42\"}}");
    }

    @Test
    void repeatedElementsBecomeArrays() {
        assertThat(decodeToString("<o><item>a</item><item>b</item><note>n</note></o>"))
                .isEqualTo("{\"o\":{\"item\":[\"a\",\"b\"],\"note\":\"n\"}}");
    }

    @Test
    void emptyElementIsEmptyString() {
        assertThat(decodeToString("<a/>")).isEqualTo("{\"a\":\"\"}");
    }

    @Test
    void whitespaceOnlyTextIsIgnored() {
        assertThat(decodeToString("<a>\n  <b>x</b>\n</a>")).isEqualTo("{\"a\":{\"b\":\"x\"}}");
    }

    @Test
    void cdataIsText() {
        assertThat(decodeToString("<a><![CDATA[1 < 2]]></a>")).isEqualTo("{\"a\":\"1 < 2\"}");
    }

    @Test
    void valuesStayStringsSoXmlNeverEqualsTypedJson() {
        CanonicalValue tree = decoder.decode(RawMessage.of("t", "<a>1</a>"));
        CanonicalValue inner = ((CanonicalObject) tree).fields().get("a");
        assertThat(inner).isEqualTo(new CanonicalString("1"));
    }

    @Test
    void doctypeIsRejected() {
        String xxe = "<!DOCTYPE foo [<!ENTITY x SYSTEM \"file:///etc/passwd\">]><a>&x;</a>";
        assertThatThrownBy(() -> decoder.decode(RawMessage.of("t", xxe)))
                .isInstanceOf(DecodeException.class);
    }

    @Test
    void invalidXmlThrows() {
        assertThatThrownBy(() -> decoder.decode(RawMessage.of("orders", "<a><b></a>")))
                .isInstanceOf(DecodeException.class)
                .hasMessageContaining("orders");
    }
}
