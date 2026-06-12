package org.foxtrot.hermetrics.rules;

import org.foxtrot.hermetrics.canonical.PathPattern;

import java.io.Serializable;

public interface NormalizationRule extends Serializable {

    PathPattern path();
}
