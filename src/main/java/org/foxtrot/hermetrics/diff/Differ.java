package org.foxtrot.hermetrics.diff;

import org.foxtrot.hermetrics.canonical.CanonicalValue;
import org.foxtrot.hermetrics.rules.RuleSet;

import java.util.List;

public interface Differ {

    List<FieldDiff> diff(CanonicalValue main, CanonicalValue load, RuleSet rules);
}
