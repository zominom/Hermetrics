package org.foxtrot.hermetrics.diff;

import org.foxtrot.hermetrics.canonical.Path;
import org.foxtrot.hermetrics.canonical.PathPattern;
import org.foxtrot.hermetrics.rules.RuleSet;
import org.foxtrot.hermetrics.rules.builtin.NumberToleranceRule;
import org.foxtrot.hermetrics.rules.builtin.TimeToleranceRule;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.foxtrot.hermetrics.testutil.Trees.json;

class StructuralDifferTest {

    private static final StructuralDiffer DIFFER = new StructuralDiffer();

    private static List<FieldDiff> diff(String main, String load) {
        return diff(main, load, RuleSet.EMPTY);
    }

    private static List<FieldDiff> diff(String main, String load, RuleSet rules) {
        return DIFFER.diff(json(main), json(load), rules);
    }

    @Test
    void equalTreesProduceNoDiffs() {
        assertThat(diff("{\"a\": 1.0, \"b\": [1, 2]}", "{\"b\": [1, 2.000], \"a\": 1}")).isEmpty();
    }

    @Test
    void valueChangeCarriesBothValues() {
        List<FieldDiff> diffs = diff("{\"a\": 1}", "{\"a\": 2}");
        assertThat(diffs).containsExactly(
                new FieldDiff(Path.ROOT.child("a"), FieldDiff.Kind.VALUE_CHANGED, "1", "2"));
    }

    @Test
    void addedAndRemovedFields() {
        List<FieldDiff> diffs = diff("{\"a\": 1, \"b\": 2}", "{\"a\": 1, \"c\": 3}");
        assertThat(diffs).containsExactlyInAnyOrder(
                new FieldDiff(Path.ROOT.child("b"), FieldDiff.Kind.REMOVED, "2", null),
                new FieldDiff(Path.ROOT.child("c"), FieldDiff.Kind.ADDED, null, "3"));
    }

    @Test
    void typeChangeIsItsOwnKind() {
        List<FieldDiff> diffs = diff("{\"a\": \"1\"}", "{\"a\": 1}");
        assertThat(diffs).singleElement()
                .extracting(FieldDiff::kind).isEqualTo(FieldDiff.Kind.TYPE_CHANGED);
    }

    @Test
    void arraysCompareByPosition() {
        List<FieldDiff> diffs = diff("{\"items\": [{\"p\": 1}, {\"p\": 2}]}",
                "{\"items\": [{\"p\": 1}, {\"p\": 3}]}");
        assertThat(diffs).singleElement()
                .extracting(diff -> diff.path().render()).isEqualTo("items[1].p");
    }

    @Test
    void extraArrayElementsAreAddedAtTheirIndex() {
        List<FieldDiff> diffs = diff("[1, 2]", "[1, 2, 3]");
        assertThat(diffs).containsExactly(
                new FieldDiff(Path.ROOT.index(2), FieldDiff.Kind.ADDED, null, "3"));
    }

    @Test
    void numberToleranceSuppressesSmallDrift() {
        RuleSet rules = RuleSet.of(new NumberToleranceRule(
                PathPattern.parse("total"), new BigDecimal("0.01")));
        assertThat(diff("{\"total\": 10.005}", "{\"total\": 10.009}", rules)).isEmpty();
        assertThat(diff("{\"total\": 10.0}", "{\"total\": 10.5}", rules)).hasSize(1);
    }

    @Test
    void timeToleranceUnderstandsIsoAndOffsets() {
        RuleSet rules = RuleSet.of(new TimeToleranceRule(
                PathPattern.parse("ts"), 5000, TimeToleranceRule.EpochUnit.MILLIS));
        assertThat(diff("{\"ts\": \"2026-06-11T10:00:00Z\"}", "{\"ts\": \"2026-06-11T10:00:03Z\"}", rules))
                .isEmpty();
        assertThat(diff("{\"ts\": \"2026-06-11T10:00:00Z\"}", "{\"ts\": \"2026-06-11T13:00:01+03:00\"}", rules))
                .isEmpty();
        assertThat(diff("{\"ts\": \"2026-06-11T10:00:00Z\"}", "{\"ts\": \"2026-06-11T10:00:10Z\"}", rules))
                .hasSize(1);
    }

    @Test
    void signatureGeneralizesArrayIndices() {
        List<FieldDiff> diffs = diff("{\"items\": [{\"x\": 1}, {\"x\": 2}, {\"x\": 3}]}",
                "{\"items\": [{\"x\": 9}, {\"x\": 2}, {\"x\": 8}]}");
        assertThat(diffs).hasSize(2);
        DiffSignature signature = DiffSignature.of(diffs);
        assertThat(signature.entries()).containsExactly("items[].x: value-changed");
    }

    @Test
    void signatureIdIsStableAcrossDiffOrder() {
        FieldDiff first = new FieldDiff(Path.ROOT.child("a"), FieldDiff.Kind.VALUE_CHANGED, "1", "2");
        FieldDiff second = new FieldDiff(Path.ROOT.child("b"), FieldDiff.Kind.ADDED, null, "3");
        assertThat(DiffSignature.of(List.of(first, second)))
                .isEqualTo(DiffSignature.of(List.of(second, first)));
    }
}
