package org.foxtrot.hermetrics.rules;

import com.fasterxml.jackson.databind.JsonNode;
import org.foxtrot.hermetrics.canonical.PathPattern;

import java.io.Serializable;

@FunctionalInterface
public interface RuleFactory extends Serializable {

    NormalizationRule create(PathPattern path, JsonNode spec);
}
