package org.foxtrot.hermetrics.canonical.path;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PathPatternTest {

    @Test
    void exactFieldPath() {
        PathPattern pattern = PathPattern.parse("meta.timestamp");
        assertThat(pattern.matches(Path.ROOT.child("meta").child("timestamp"))).isTrue();
        assertThat(pattern.matches(Path.ROOT.child("meta").child("other"))).isFalse();
        assertThat(pattern.matches(Path.ROOT.child("meta"))).isFalse();
    }

    @Test
    void anyIndexMatchesAllPositions() {
        PathPattern pattern = PathPattern.parse("items[].id");
        assertThat(pattern.matches(Path.ROOT.child("items").index(0).child("id"))).isTrue();
        assertThat(pattern.matches(Path.ROOT.child("items").index(7).child("id"))).isTrue();
        assertThat(pattern.matches(Path.ROOT.child("items").child("id"))).isFalse();
    }

    @Test
    void concreteIndexMatchesOnlyThatPosition() {
        PathPattern pattern = PathPattern.parse("items[2]");
        assertThat(pattern.matches(Path.ROOT.child("items").index(2))).isTrue();
        assertThat(pattern.matches(Path.ROOT.child("items").index(1))).isFalse();
    }

    @Test
    void starMatchesExactlyOneField() {
        PathPattern pattern = PathPattern.parse("*.id");
        assertThat(pattern.matches(Path.ROOT.child("order").child("id"))).isTrue();
        assertThat(pattern.matches(Path.ROOT.child("a").child("b").child("id"))).isFalse();
        assertThat(pattern.matches(Path.ROOT.index(0).child("id"))).isFalse();
    }

    @Test
    void deepMatchesAnyDepthIncludingTopLevel() {
        PathPattern pattern = PathPattern.parse("**.traceId");
        assertThat(pattern.matches(Path.ROOT.child("traceId"))).isTrue();
        assertThat(pattern.matches(Path.ROOT.child("a").child("b").child("traceId"))).isTrue();
        assertThat(pattern.matches(Path.ROOT.child("items").index(0).child("traceId"))).isTrue();
        assertThat(pattern.matches(Path.ROOT.child("traceId2"))).isFalse();
    }

    @Test
    void deepInTheMiddle() {
        PathPattern pattern = PathPattern.parse("a.**.z");
        assertThat(pattern.matches(Path.ROOT.child("a").child("z"))).isTrue();
        assertThat(pattern.matches(Path.ROOT.child("a").child("b").child("c").child("z"))).isTrue();
        assertThat(pattern.matches(Path.ROOT.child("x").child("z"))).isFalse();
    }

    @Test
    void dollarDenotesRoot() {
        assertThat(PathPattern.parse("$").matches(Path.ROOT)).isTrue();
        assertThat(PathPattern.parse("$.a").matches(Path.ROOT.child("a"))).isTrue();
        assertThat(PathPattern.parse("$[0]").matches(Path.ROOT.index(0))).isTrue();
    }

    @Test
    void escapedDotIsPartOfTheFieldName() {
        PathPattern pattern = PathPattern.parse("a\\.b");
        assertThat(pattern.matches(Path.ROOT.child("a.b"))).isTrue();
        assertThat(pattern.matches(Path.ROOT.child("a").child("b"))).isFalse();
    }

    @Test
    void emptySegmentsAreRejected() {
        assertThatThrownBy(() -> PathPattern.parse("a..b")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> PathPattern.parse("a.")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> PathPattern.parse("a[1")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void concretePathParsesAndRenders() {
        Path path = Path.parse("a.b[2].c");
        assertThat(path.render()).isEqualTo("a.b[2].c");
        assertThat(path.generalized()).isEqualTo("a.b[].c");
        assertThat(Path.ROOT.render()).isEqualTo("$");
    }

    @Test
    void concretePathRejectsWildcards() {
        assertThatThrownBy(() -> Path.parse("a[].b")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Path.parse("**.b")).isInstanceOf(IllegalArgumentException.class);
    }
}
