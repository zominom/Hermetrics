package org.foxtrot.hermetrics.report;

import org.foxtrot.hermetrics.match.verdict.Verdict;

import java.io.Serializable;

public interface FindingCodec extends Serializable {

    String verdict(Verdict verdict);

    String deadLetter(DeadLetter deadLetter);

    String rollup(Rollup rollup);
}
