package com.couchbase.workloads.impl;

public class FiveMinuteIntervalFriendFilterReplication extends
        ContinuousFriendFilterReplication {

    @Override
    protected boolean shouldPullContinuously() {
        return false;
    }

    @Override
    protected boolean shouldPushContinuously() {
        return false;
    }

    @Override
    protected long intervalBetweenPull() {
        return 1 * 60 * 5000;
    }

    @Override
    protected long intervalBetweenPush() {
        return 1 * 60 * 5000;
    }

}
