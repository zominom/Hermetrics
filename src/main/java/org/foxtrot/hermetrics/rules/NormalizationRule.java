package org.foxtrot.hermetrics.rules;

import org.foxtrot.hermetrics.canonical.path.PathPattern;

import java.io.Serializable;

public interface NormalizationRule extends Serializable {

    PathPattern path();
}
