package org.foxtrot.hermetrics.canonical;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.foxtrot.hermetrics.testutil.Trees.json;

class CanonicalValueTest {

    @Test
    void numbersEqualRegardlessOfRepresentation() {
        assertThat(json("{\"a\": 1}")).isEqualTo(json("{\"a\": 1.000}"));
        assertThat(json("{\"a\": 100}")).isEqualTo(json("{\"a\": 1e2}"));
        assertThat(json("{\"a\": 0.5}")).isNotEqualTo(json("{\"a\": 0.50001}"));
    }

    @Test
    void writerSortsObjectKeys() {
        assertThat(CanonicalJsonWriter.write(json("{\"b\": 2, \"a\": 1}")))
                .isEqualTo("{\"a\":1,\"b\":2}");
    }

    @Test
    void writerRendersScalarsAndArrays() {
        assertThat(CanonicalJsonWriter.write(json("[null, true, \"x\", 1.50]")))
                .isEqualTo("[null,true,\"x\",1.5]");
    }

    @Test
    void writerEscapesStrings() {
        String rendered = CanonicalJsonWriter.write(new CanonicalString("he said \"hi\"\nback\\slash"));
        assertThat(rendered).isEqualTo("\"he said \\\"hi\\\"\\nback\\\\slash\"");
    }

    @Test
    void readerRoundTripsTheCanonicalForm() {
        CanonicalValue tree = json("{\"b\": 1.0, \"a\": [1, \"x\", null, true]}");
        assertThat(CanonicalJsonReader.parse(CanonicalJsonWriter.write(tree))).isEqualTo(tree);
    }

    @Test
    void hashIgnoresKeyOrderAndNumberForm() {
        String a = ContentHasher.hash(json("{\"b\": 1.0, \"a\": [1, 2]}"));
        String b = ContentHasher.hash(json("{\"a\": [1, 2.000], \"b\": 1}"));
        assertThat(a).isEqualTo(b);
    }

    @Test
    void hashDiffersForDifferentContent() {
        assertThat(ContentHasher.hash(json("{\"a\": 1}")))
                .isNotEqualTo(ContentHasher.hash(json("{\"a\": 2}")));
    }

    @Test
    void nullsAreEqual() {
        assertThat(CanonicalNull.INSTANCE).isEqualTo(new CanonicalNull());
    }
}
