package com.couchbase.javatester;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.ektorp.CouchDbConnector;
import org.ektorp.CouchDbInstance;
import org.ektorp.http.HttpClient;
import org.ektorp.http.StdHttpClient;
import org.ektorp.impl.StdCouchDbInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.couchbase.util.ChangesFeedMonitor;
import com.couchbase.workloads.CouchbaseWorkload;
import com.couchbase.workloads.CouchbaseWorkloadRunner;
import com.couchbase.workloads.WorkloadHelper;

public class JavaTester implements CouchbaseWorkloadRunner {

    private final static Logger LOG = LoggerFactory
            .getLogger(JavaTester.class);

    private static final String TAG = "JavaTester";

    private List<CouchbaseWorkload> workloads = new ArrayList<CouchbaseWorkload>();
    private List<String> nodeUrls = new ArrayList<String>();
    private String workloadReplicaitonUrl;
    private String logReplicationUrl;
    private Map<String, String> changeIdRevisions = new HashMap<String,String>();
    private Map<String, Long> changeIdTimestamps = new HashMap<String,Long>();
    private Map<String, Map<String, Long>> changeIdDeviceTimings = new HashMap<String, Map<String,Long>>();
    private int numFriends = 2;

    public static void usage() {
        System.out.println("JavaTester <options>");
        System.out.println("\t-workload <comma-delimited list of workloads>  *REQUIRED*");
        System.out.println("\t-workload_sync_url <url>");
        System.out.println("\t-log_sync_url <url>");
        System.out.println("\t-min_delay <delay in ms>");
        System.out.println("\t-num_friends <number of friends to tag in documents>");
    }

    public JavaTester(String workloadReplicaitonUrl, String logReplicationUrl, int numFriends) {
        this.workloadReplicaitonUrl = workloadReplicaitonUrl;
        this.logReplicationUrl = logReplicationUrl;
        this.numFriends = numFriends;
    }

    public static void main(String[] args) throws Exception {

        String startWorkloadString = null;
        String workloadSyncUrl = null;
        String logSyncUrl = null;
        int minimumDelayArgument = 500;
        int numFriendsArgument = 2;

        int i = 0;
        String arg = null;
        while (i < args.length && args[i].startsWith("-")) {
            arg = args[i++];

            if(arg.equals("-workload")) {
                if (i < args.length) {
                    startWorkloadString = args[i++];
                }
                else {
                    System.err.println("-workload requires a string argument");
                    System.exit(1);
                }
            }
            else if(arg.equals("-workload_sync_url")) {
                if (i < args.length) {
                    workloadSyncUrl = args[i++];
                    System.out.println("Workload Sync URL: " + workloadSyncUrl);
                }
                else {
                    System.err.println("-workload_sync_url requires a string argument");
                    System.exit(1);
                }
            }
            else if(arg.equals("-log_sync_url")) {
                if (i < args.length) {
                    logSyncUrl = args[i++];
                    System.out.println("Log Sync URL: " + logSyncUrl);
                }
                else {
                    System.err.println("-log_sync_url requires a string argument");
                    System.exit(1);
                }
            }
            else if(arg.equals("-min_delay")) {
                if (i < args.length) {
                    minimumDelayArgument = Integer.parseInt(args[i++]);
                    System.out.println("Minimum Delay: " + minimumDelayArgument);
                }
                else {
                    System.err.println("-min_delay requires an integer argument");
                    System.exit(1);
                }
            }
            else if(arg.equals("-num_friends")) {
                if (i < args.length) {
                    numFriendsArgument = Integer.parseInt(args[i++]);
                    System.out.println("Number of Friends: " + numFriendsArgument);
                }
                else {
                    System.err.println("-num_friends requires an integer argument");
                    System.exit(1);
                }
            }

        }

        //startWorkloadString is required
        if(startWorkloadString == null) {
            usage();
            System.exit(1);
        }


        JavaTester tester = new JavaTester(workloadSyncUrl, logSyncUrl, numFriendsArgument);
        tester.run(startWorkloadString, minimumDelayArgument);
    }

    public void run(String startWorkloadString, int minimumDelay) throws Exception {
        List<String> startWorkloads = null;
        if(startWorkloadString != null) {
            LOG.debug(TAG, "Requested to start workload " + startWorkloadString);
            System.out.println("Starting Workloads: " + startWorkloadString);
            startWorkloads = Arrays.asList(startWorkloadString.split(","));
        }
        else {
            System.out.println("Must provide at least one workload to run.");
            System.exit(1);
        }

        //create a changes feed monitor for the cloud
        ChangesFeedMonitor cloudFeedMonitor = new ChangesFeedMonitor(this, getWorkloadReplicationUrl());
        cloudFeedMonitor.start();


        BufferedReader stdin = new BufferedReader(new InputStreamReader(System.in));
        String str;
        while ((str = stdin.readLine()) != null) {
            //expect each line to be a URL
            nodeUrls.add(str);
        }

        for (String nodeUrl : nodeUrls) {

            //start the workloads on the node
            HttpClient httpClient = buildHttpClientFromUrl(nodeUrl);
            CouchDbInstance couchDbInstance = new StdCouchDbInstance(httpClient);

            String dbName = getDatabaseNameFromUrl(nodeUrl);
            if(dbName == null || dbName.equals("")) {
                dbName = WorkloadHelper.DEFAULT_WORKLOAD_DB;
            }
            CouchDbConnector couchDbConnector = couchDbInstance.createConnector(dbName, true);

            //start a process to follow changes feed on the node (do this after connector was created syncronously, no contention creating the db
            ChangesFeedMonitor deviceFeedMonitor = new ChangesFeedMonitor(this, nodeUrl);
            deviceFeedMonitor.start();


            //now actually start the workloads
            for(String workloadName : startWorkloads) {
                CouchbaseWorkload workload = WorkloadHelper.loadWorkload(workloadName);
                workload.setCouchDbInstance(couchDbInstance);
                workload.setCouchDbConnector(couchDbConnector);
                workload.setCouchbaseWorkloadRunner(this);
                workload.addExtra(WorkloadHelper.EXTRA_WORKLOAD_DB, dbName);
                workload.addExtra(WorkloadHelper.EXTRA_NODE_ID, nodeUrl);
                workload.addExtra(WorkloadHelper.EXTRA_MINIMUM_DELAY, minimumDelay);
                workload.addExtra(WorkloadHelper.EXTRA_NUM_FRIENDS, numFriends);
                LOG.debug(TAG, "Starting workload " + workload.getName());
                workload.start();
                //add to our list
                workloads.add(workload);
            }
        }

        //wait for all workloads to finish (they never will)
        for (CouchbaseWorkload workload : workloads) {
            workload.waitForCompletion();
        }
    }

    //implementation of CouchbaseWorkloadRunner interface

    @Override
    public InputStream openResource(String path) throws IOException {
        File resource = new File("assets/" + path);
        FileInputStream is = new FileInputStream(resource);
        return is;
    }

    @Override
    public String getLogsReplicationUrl() {
        String result = logReplicationUrl;
        if(result == null) {
            result = WorkloadHelper.DEFAULT_LOGS_SYNC_URL;
        }
        return result;
    }

    @Override
    public String getWorkloadReplicationUrl() {
        String result = workloadReplicaitonUrl;
        if(result == null) {
            result = WorkloadHelper.DEFAULT_WORKLOAD_SYNC_URL;
        }
        return result;
    }

    @Override
    public void publishedWorkloadDocumentWithIdandRevision(String id, String rev) {
        long currentTime = System.currentTimeMillis();
        synchronized (this) {
            //place this id and timestamp in both maps
            //if we were track older revision it will get overwritten
            changeIdRevisions.put(id, rev);
            changeIdTimestamps.put(id, currentTime);
            changeIdDeviceTimings.put(id, new HashMap<String,Long>());
        }
    }

    public void recordDeviceSeesDocumentWithIdAndRevision(String deviceId, String documentId, String revision) {
        long currentTime = System.currentTimeMillis();
        long createTime  = -1L;
        Map<String,Long> deviceTimings = null;
        String trackedRev = null;

        synchronized (this) {
            //first make sure this is the revision we're interested in
            trackedRev = changeIdRevisions.get(documentId);
            if((trackedRev != null) && (trackedRev.equals(revision))) {
                deviceTimings = changeIdDeviceTimings.get(documentId);
                if(deviceTimings != null) {
                    deviceTimings.put(deviceId, currentTime);
                }

                //we expect to receive num_friends + 1 (for the cloud) entries
                if(deviceTimings.size() == (numFriends + 1) ) {
                    createTime = changeIdTimestamps.remove(documentId);

                    //cleanup everything else associated with this key
                    changeIdRevisions.remove(documentId);
                    changeIdDeviceTimings.remove(documentId);

                }
            }
        }

        //now print out (after out of synchronized block)
        if(createTime > -1L && deviceTimings != null) {
            //first print the cloud time
            Long cloudTime = deviceTimings.remove(getWorkloadReplicationUrl());
            if(cloudTime == null) {
                System.out.println("ERROR clould time is null");
            }
            System.out.print(documentId + ",");
            System.out.print(cloudTime - createTime);
            for (String deviceKey : deviceTimings.keySet()) {
                Long deviceTime = deviceTimings.get(deviceKey);

                System.out.print(",");
                System.out.print(deviceTime - createTime);
            }
            System.out.println();
        }

    }

    @Override
    public HttpClient buildHttpClientFromUrl(String urlString) throws MalformedURLException {

        URL url = new URL(urlString);
        String host = url.getHost();
        int port = url.getPort();
        if(port < 0) {
            port = 80;
        }

        return new StdHttpClient.Builder().host(host).port(port).build();
    }

    @Override
    public String getDatabaseNameFromUrl(String urlString) throws MalformedURLException {

        URL url = new URL(urlString);
        String path = url.getPath();
        if(path.startsWith("/")) {
            path = path.substring(1);
        }

        return path;
    }

    @Override
    public List<String> getRandomFriends(String self, int count) {
        List<String> result = new ArrayList<String>();

        Random r = new Random();
        while(result.size() < count) {
            int randomIndex = r.nextInt(nodeUrls.size());
            String candidateFriend = nodeUrls.get(randomIndex);
            //make sure friend is not self or already in the list
            if(!candidateFriend.equals(self) && !result.contains(candidateFriend)) {
                result.add(candidateFriend);
            }

        }

        return result;
    }

}
