package org.foxtrot.hermetrics.match.state;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TimelineTest {

    private final Timeline timeline = new Timeline();

    @Test
    void unsequencedKeepsOnlyTheTailPayload() {
        timeline.observe(null, "h1", "{\"v\":1}", 1);
        timeline.observe(null, "h2", "{\"v\":2}", 2);
        assertThat(timeline.versions.get(0).stateJson).isNull();
        assertThat(timeline.finalVersion().stateJson).isEqualTo("{\"v\":2}");
        assertThat(timeline.distinctStates()).isEqualTo(2);
    }

    @Test
    void unsequencedRepeatedHashIsADuplicate() {
        timeline.observe(null, "h1", "{}", 1);
        timeline.observe(null, "h1", "{}", 2);
        assertThat(timeline.duplicateCount).isEqualTo(1);
        assertThat(timeline.distinctStates()).isEqualTo(1);
    }

    @Test
    void sequencedFinalIsTheNumericallyHighestSequence() {
        timeline.observe("10", "h10", "{\"v\":10}", 1);
        timeline.observe("9", "h9", "{\"v\":9}", 2);
        assertThat(timeline.finalVersion().sequence).isEqualTo("10");
        assertThat(timeline.finalVersion().stateJson).isEqualTo("{\"v\":10}");
    }

    @Test
    void nonNumericSequencesFallBackToLexicographicOrder() {
        timeline.observe("b", "hb", "{}", 1);
        timeline.observe("a", "ha", "{}", 2);
        assertThat(timeline.finalVersion().sequence).isEqualTo("b");
    }

    @Test
    void sameSequenceSameContentIsADuplicate() {
        timeline.observe("1", "h1", "{}", 1);
        timeline.observe("1", "h1", "{}", 2);
        assertThat(timeline.duplicateCount).isEqualTo(1);
        assertThat(timeline.distinctStates()).isEqualTo(1);
    }

    @Test
    void sameSequenceNewContentKeepsTheLatestByTime() {
        timeline.observe("1", "hA", "{\"v\":\"A\"}", 10);
        timeline.observe("1", "hB", "{\"v\":\"B\"}", 20);
        assertThat(timeline.bySequence("1").hash).isEqualTo("hB");

        timeline.observe("1", "hStale", "{\"v\":\"stale\"}", 5);
        assertThat(timeline.bySequence("1").hash).isEqualTo("hB");
    }

    @Test
    void sequenceComparatorIsNumericAware() {
        assertThat(Timeline.compareSequences("9", "10")).isNegative();
        assertThat(Timeline.compareSequences("2.5", "2.50")).isZero();
        assertThat(Timeline.compareSequences("a10", "a2")).isNegative();
    }
}
