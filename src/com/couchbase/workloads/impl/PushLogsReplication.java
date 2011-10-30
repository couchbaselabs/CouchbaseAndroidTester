package com.couchbase.workloads.impl;

import org.ektorp.DbAccessException;
import org.ektorp.ReplicationCommand;
import org.ektorp.ReplicationStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.couchbase.workloads.CouchbaseWorkload;
import com.couchbase.workloads.WorkloadHelper;

public class PushLogsReplication extends CouchbaseWorkload {

    private final static Logger LOG = LoggerFactory
            .getLogger(PushLogsReplication.class);

    @Override
    protected String performWork() {

        ReplicationCommand pushReplicationCommand = new ReplicationCommand.Builder()
        .source(WorkloadHelper.TEST_RESULTS_DB)
        .target(workloadRunner.getLogsReplicationUrl())
        .continuous(true)
        .build();

        LOG.debug("Starting Continuous Push Replication of Logs");
        ReplicationStatus pushStatus;
        try {
            pushStatus = couchDbInstance.replicate(pushReplicationCommand);
            LOG.debug("Finished Replication of Logs: " + pushStatus.isOk());
        } catch (DbAccessException e) {
            LOG.debug("Replication of Logs Error: ", e);
        }

        while(!thread.isCancelled()) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
              //ignore
            }
        }

        return "Continuous Push Replication of Logs Workload was cancelled";

    }

    @Override
    public String getName() {
        return "Continuous Push Replication of Logs";
    }

}

