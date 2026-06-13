package org.foxtrot.hermetrics.diff.algorithm;

import org.foxtrot.hermetrics.diff.Differ;
import org.foxtrot.hermetrics.diff.FieldDiff;

import org.foxtrot.hermetrics.canonical.value.CanonicalArray;
import org.foxtrot.hermetrics.canonical.json.CanonicalJsonWriter;
import org.foxtrot.hermetrics.canonical.value.CanonicalObject;
import org.foxtrot.hermetrics.canonical.value.CanonicalValue;
import org.foxtrot.hermetrics.canonical.path.Path;
import org.foxtrot.hermetrics.rules.EquivalenceRule;
import org.foxtrot.hermetrics.rules.RuleSet;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

public final class StructuralDiffer implements Differ {

    @Override
    public List<FieldDiff> diff(CanonicalValue main, CanonicalValue load, RuleSet rules) {
        List<FieldDiff> out = new ArrayList<>();
        walk(Path.ROOT, main, load, rules, out);
        return out;
    }

    private void walk(Path path, CanonicalValue main, CanonicalValue load, RuleSet rules, List<FieldDiff> out) {
        if (main.equals(load) || equivalentByRule(path, main, load, rules)) {
            return;
        }
        if (main instanceof CanonicalObject mainObj && load instanceof CanonicalObject loadObj) {
            walkObjects(path, mainObj, loadObj, rules, out);
        } else if (main instanceof CanonicalArray mainArr && load instanceof CanonicalArray loadArr) {
            walkArrays(path, mainArr, loadArr, rules, out);
        } else if (main.typeName().equals(load.typeName())) {
            out.add(new FieldDiff(path, FieldDiff.Kind.VALUE_CHANGED,
                    CanonicalJsonWriter.write(main), CanonicalJsonWriter.write(load)));
        } else {
            out.add(new FieldDiff(path, FieldDiff.Kind.TYPE_CHANGED,
                    CanonicalJsonWriter.write(main), CanonicalJsonWriter.write(load)));
        }
    }

    private static boolean equivalentByRule(Path path, CanonicalValue main, CanonicalValue load, RuleSet rules) {
        for (EquivalenceRule rule : rules.equivalenceRules()) {
            if (rule.path().matches(path) && rule.equivalent(main, load)) {
                return true;
            }
        }
        return false;
    }

    private void walkObjects(Path path, CanonicalObject main, CanonicalObject load,
                             RuleSet rules, List<FieldDiff> out) {
        TreeSet<String> keys = new TreeSet<>(main.fields().keySet());
        keys.addAll(load.fields().keySet());
        for (String key : keys) {
            CanonicalValue mainValue = main.fields().get(key);
            CanonicalValue loadValue = load.fields().get(key);
            if (mainValue == null) {
                out.add(new FieldDiff(path.child(key), FieldDiff.Kind.ADDED,
                        null, CanonicalJsonWriter.write(loadValue)));
            } else if (loadValue == null) {
                out.add(new FieldDiff(path.child(key), FieldDiff.Kind.REMOVED,
                        CanonicalJsonWriter.write(mainValue), null));
            } else {
                walk(path.child(key), mainValue, loadValue, rules, out);
            }
        }
    }

    private void walkArrays(Path path, CanonicalArray main, CanonicalArray load,
                            RuleSet rules, List<FieldDiff> out) {
        int common = Math.min(main.items().size(), load.items().size());
        for (int i = 0; i < common; i++) {
            walk(path.index(i), main.items().get(i), load.items().get(i), rules, out);
        }
        for (int i = common; i < main.items().size(); i++) {
            out.add(new FieldDiff(path.index(i), FieldDiff.Kind.REMOVED,
                    CanonicalJsonWriter.write(main.items().get(i)), null));
        }
        for (int i = common; i < load.items().size(); i++) {
            out.add(new FieldDiff(path.index(i), FieldDiff.Kind.ADDED,
                    null, CanonicalJsonWriter.write(load.items().get(i))));
        }
    }
}
