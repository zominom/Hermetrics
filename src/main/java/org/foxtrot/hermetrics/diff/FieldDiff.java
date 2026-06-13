package org.foxtrot.hermetrics.diff;

import org.foxtrot.hermetrics.canonical.path.Path;

import java.io.Serializable;

public record FieldDiff(Path path, Kind kind, String mainValue, String loadValue) implements Serializable {

    public enum Kind {
        ADDED("added"),
        REMOVED("removed"),
        TYPE_CHANGED("type-changed"),
        VALUE_CHANGED("value-changed");

        private final String label;

        Kind(String label) {
            this.label = label;
        }

        public String label() {
            return label;
        }
    }
}
