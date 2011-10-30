package com.couchbase.workloads.impl;


public class FiveMinuteIntervalReplication extends AbstractReplication {

    @Override
    protected boolean shouldPullFromCloud() {
        return true;
    }

    @Override
    protected boolean shouldPushToCloud() {
        return true;
    }

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

    @Override
    protected String pullFilterFunction() {
        return null;
    }

    @Override
    protected String pushFilterFunction() {
        return null;
    }

    @Override
    protected Object pullQueryParams() {
        return null;
    }

    @Override
    protected Object pushQueryParams() {
        return null;
    }

    @Override
    public String getName() {
        return "Five Minute Interval Replication";
    }

}
