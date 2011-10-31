package com.couchbase.util;

import java.net.MalformedURLException;

import org.ektorp.CouchDbConnector;
import org.ektorp.CouchDbInstance;
import org.ektorp.DbInfo;
import org.ektorp.changes.ChangesCommand;
import org.ektorp.changes.ChangesFeed;
import org.ektorp.changes.DocumentChange;
import org.ektorp.http.HttpClient;
import org.ektorp.impl.StdCouchDbInstance;

import com.couchbase.workloads.CouchbaseWorkloadRunner;
import com.couchbase.workloads.WorkloadHelper;

public class ChangesFeedMonitor extends Thread {

    private CouchbaseWorkloadRunner workloadRunner;
    private String deviceUrl;

    public ChangesFeedMonitor(CouchbaseWorkloadRunner workloadRunner, String deviceUrl) {
        this.workloadRunner = workloadRunner;
        this.deviceUrl = deviceUrl;
    }

    @Override
    public void run() {

        try {
            HttpClient httpClient = workloadRunner.buildHttpClientFromUrl(deviceUrl);
            CouchDbInstance couchDbInstance = new StdCouchDbInstance(httpClient);

            String dbName = workloadRunner.getDatabaseNameFromUrl(deviceUrl);
            if(dbName == null || dbName.equals("")) {
                dbName = WorkloadHelper.DEFAULT_WORKLOAD_DB;
            }
            CouchDbConnector couchDbConnector = couchDbInstance.createConnector(dbName, true);

            DbInfo dbInfo = couchDbConnector.getDbInfo();
            long lastUpdateSeq = dbInfo.getUpdateSeq();

            ChangesCommand cmd = new ChangesCommand.Builder()
                                                   .since(lastUpdateSeq)
                                                   .includeDocs(false)
                                                   .build();

            ChangesFeed feed = couchDbConnector.changesFeed(cmd);

            while (feed.isAlive()) {
                DocumentChange change = feed.next();
                String id = change.getId();
                String rev = change.getRevision();
                int dashLocation = rev.indexOf("-");
                String choppedRev = rev.substring(0, dashLocation);
                workloadRunner.recordDeviceSeesDocumentWithIdAndRevision(deviceUrl, id, choppedRev);

            }

        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            //ignore
        }

    }

}
