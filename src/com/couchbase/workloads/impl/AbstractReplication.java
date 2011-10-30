package com.couchbase.workloads.impl;

import org.ektorp.DbAccessException;
import org.ektorp.ReplicationCommand;
import org.ektorp.ReplicationStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.couchbase.androidtester.CouchbaseAndroidTesterActivity;
import com.couchbase.workloads.CouchbaseWorkload;
import com.couchbase.workloads.WorkloadHelper;

public abstract class AbstractReplication extends CouchbaseWorkload {

    private final static Logger LOG = LoggerFactory
            .getLogger(ContinuousReplication.class);

    private String workloadDb = WorkloadHelper.DEFAULT_WORKLOAD_DB;

    @Override
    protected String performWork() {

        //support a different name for the workload db
        if(extras.containsKey(WorkloadHelper.EXTRA_WORKLOAD_DB)) {
            workloadDb = (String)extras.get(WorkloadHelper.EXTRA_WORKLOAD_DB);
        }

        setupReplicationFilters();

        if(shouldPushToCloud()) {
            new Thread(new Runnable() {

                @Override
                public void run() {
                    while(!thread.isCancelled()) {

                        ReplicationCommand.Builder pushReplicationCommandBuilder = new ReplicationCommand.Builder()
                        .source(workloadDb)
                        .target(workloadRunner.getWorkloadReplicationUrl())
                        .continuous(shouldPushContinuously());

                        if(pushFilterFunction() != null) {
                            pushReplicationCommandBuilder.filter(pushFilterFunction());
                        }

                        if(pushQueryParams() != null) {
                            pushReplicationCommandBuilder.queryParams(pushQueryParams());
                        }

                        ReplicationCommand pushReplicationCommand = pushReplicationCommandBuilder.build();

                        LOG.debug(CouchbaseAndroidTesterActivity.TAG, "Starting Push Replication");
                        ReplicationStatus pushStatus;
                        try {
                            pushStatus = couchDbInstance.replicate(pushReplicationCommand);
                            LOG.debug(CouchbaseAndroidTesterActivity.TAG, "Finished Replication: " + pushStatus.isOk());
                        } catch (DbAccessException e) {
                            LOG.debug(CouchbaseAndroidTesterActivity.TAG, "Replication Error: ", e);
                        }

                        try {
                            Thread.sleep(intervalBetweenPush());
                        } catch (InterruptedException e) {
                            //ignore
                        }

                    }
                }
            }).start();
        }

        if(shouldPullFromCloud()) {
            while(!thread.isCancelled()) {
                ReplicationCommand.Builder pullReplicationCommandBuilder = new ReplicationCommand.Builder()
                .source(workloadRunner.getWorkloadReplicationUrl())
                .target(workloadDb)
                .continuous(shouldPullContinuously());

                if(pullFilterFunction() != null) {
                    pullReplicationCommandBuilder.filter(pullFilterFunction());
                }

                if(pullQueryParams() != null) {
                    pullReplicationCommandBuilder.queryParams(pullQueryParams());
                }

                ReplicationCommand pullReplicationCommand = pullReplicationCommandBuilder.build();

                LOG.debug(CouchbaseAndroidTesterActivity.TAG, "Starting Pull Replication");
                ReplicationStatus pullStatus;
                try {
                    pullStatus = couchDbInstance.replicate(pullReplicationCommand);
                    LOG.debug(CouchbaseAndroidTesterActivity.TAG, "Finished Replication: " + pullStatus.isOk());
                } catch (DbAccessException e) {
                    LOG.debug(CouchbaseAndroidTesterActivity.TAG, "Replication Error: ", e);
                }

                try {
                    Thread.sleep(intervalBetweenPull());
                } catch (InterruptedException e) {
                    //ignore
                }
            }
        }

        return null;
    }

    protected void setupReplicationFilters() {

    }

    protected abstract boolean shouldPushToCloud();

    protected abstract boolean shouldPullFromCloud();

    protected abstract boolean shouldPushContinuously();

    protected abstract boolean shouldPullContinuously();

    protected abstract long intervalBetweenPush();

    protected abstract long intervalBetweenPull();

    protected abstract String pushFilterFunction();

    protected abstract String pullFilterFunction();

    protected abstract Object pushQueryParams();

    protected abstract Object pullQueryParams();

}
