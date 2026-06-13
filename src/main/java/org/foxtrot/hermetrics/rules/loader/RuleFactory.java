package org.foxtrot.hermetrics.rules.loader;

import org.foxtrot.hermetrics.rules.NormalizationRule;

import com.fasterxml.jackson.databind.JsonNode;
import org.foxtrot.hermetrics.canonical.path.PathPattern;

import java.io.Serializable;

@FunctionalInterface
public interface RuleFactory extends Serializable {

    NormalizationRule create(PathPattern path, JsonNode spec);
}
