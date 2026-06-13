package org.foxtrot.hermetrics.rules;

import org.foxtrot.hermetrics.canonical.json.CanonicalJsonWriter;
import org.foxtrot.hermetrics.canonical.value.CanonicalNull;
import org.foxtrot.hermetrics.canonical.value.CanonicalValue;
import org.foxtrot.hermetrics.canonical.json.ContentHasher;
import org.foxtrot.hermetrics.canonical.path.PathPattern;
import org.foxtrot.hermetrics.rules.builtin.IgnoreRule;
import org.foxtrot.hermetrics.rules.builtin.MaskRule;
import org.foxtrot.hermetrics.rules.builtin.UnorderedRule;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.foxtrot.hermetrics.testutil.Trees.json;

class NormalizerTest {

    private static final Normalizer NORMALIZER = new Normalizer();

    private static String normalize(String json, RuleSet rules) {
        return CanonicalJsonWriter.write(NORMALIZER.normalize(json(json), rules));
    }

    @Test
    void ignoreRemovesMatchesAtAnyDepth() {
        RuleSet rules = RuleSet.of(new IgnoreRule(PathPattern.parse("**.traceId")));
        assertThat(normalize("{\"traceId\": \"x\", \"a\": {\"traceId\": \"y\", \"keep\": 1}}", rules))
                .isEqualTo("{\"a\":{\"keep\":1}}");
    }

    @Test
    void maskHidesTheValueButKeepsTheField() {
        RuleSet rules = RuleSet.of(new MaskRule(PathPattern.parse("customer.ssn")));
        assertThat(normalize("{\"customer\": {\"ssn\": \"123-45-6789\", \"name\": \"Ada\"}}", rules))
                .isEqualTo("{\"customer\":{\"name\":\"Ada\",\"ssn\":\"***\"}}");
    }

    @Test
    void nullFieldsAreDroppedByDefaultButArrayNullsStay() {
        assertThat(normalize("{\"a\": null, \"b\": [null, 1]}", RuleSet.EMPTY))
                .isEqualTo("{\"b\":[null,1]}");
    }

    @Test
    void nullFieldsAreKeptWhenDisabled() {
        RuleSet keepNulls = new RuleSet(false, false, List.of());
        assertThat(normalize("{\"a\": null}", keepNulls)).isEqualTo("{\"a\":null}");
    }

    @Test
    void emptyContainersDropWhenEnabled() {
        RuleSet rules = new RuleSet(true, true, List.of());
        assertThat(normalize("{\"a\": [], \"b\": {}, \"c\": 1}", rules)).isEqualTo("{\"c\":1}");
    }

    @Test
    void unorderedSortsAfterChildNormalization() {
        RuleSet rules = RuleSet.of(
                new IgnoreRule(PathPattern.parse("items[].ts")),
                new UnorderedRule(PathPattern.parse("items")));
        CanonicalValue a = NORMALIZER.normalize(
                json("{\"items\": [{\"id\": 2, \"ts\": \"x\"}, {\"id\": 1, \"ts\": \"y\"}]}"), rules);
        CanonicalValue b = NORMALIZER.normalize(
                json("{\"items\": [{\"id\": 1, \"ts\": \"z\"}, {\"id\": 2, \"ts\": \"w\"}]}"), rules);
        assertThat(a).isEqualTo(b);
        assertThat(ContentHasher.hash(a)).isEqualTo(ContentHasher.hash(b));
    }

    @Test
    void ignoringTheRootYieldsNull() {
        RuleSet rules = RuleSet.of(new IgnoreRule(PathPattern.parse("$")));
        assertThat(NORMALIZER.normalize(json("{\"a\": 1}"), rules)).isEqualTo(CanonicalNull.INSTANCE);
    }
}
