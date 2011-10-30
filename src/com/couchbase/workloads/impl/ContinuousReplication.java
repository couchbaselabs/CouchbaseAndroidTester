package com.couchbase.workloads.impl;


public class ContinuousReplication extends AbstractReplication {

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
        return true;
    }

    @Override
    protected boolean shouldPushContinuously() {
        return true;
    }

    @Override
    protected long intervalBetweenPull() {
        return 30 * 1000;
    }

    @Override
    protected long intervalBetweenPush() {
        return 30 * 1000;
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
        return "Continuous Replication";
    }

}
