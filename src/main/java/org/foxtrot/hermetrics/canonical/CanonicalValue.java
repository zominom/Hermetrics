package org.foxtrot.hermetrics.canonical;

import java.io.Serializable;

public sealed interface CanonicalValue extends Serializable
        permits CanonicalObject, CanonicalArray, CanonicalString, CanonicalNumber, CanonicalBool, CanonicalNull {

    String typeName();
}
