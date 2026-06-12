package org.foxtrot.hermetrics.match;

import java.io.Serializable;

public final class TopicPair implements Serializable {

    public Timeline main = new Timeline();
    public Timeline load = new Timeline();
    public int revision;

    public Timeline timeline(Env env) {
        return env == Env.MAIN ? main : load;
    }
}
