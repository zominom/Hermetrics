package org.foxtrot.hermetrics.flink.operator;

import java.io.Serializable;
import java.util.ArrayList;

public final class RollupAccumulator implements Serializable {

    public String topic = "";
    public String status = "";
    public String severity = "";
    public String signatureId = "";
    public String signatureText = "";
    public long count;
    public ArrayList<String> sampleGuids = new ArrayList<>();
}
