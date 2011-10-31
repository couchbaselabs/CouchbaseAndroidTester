package com.couchbase.workloads;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WorkloadHelper {

    private final static Logger LOG = LoggerFactory
            .getLogger(WorkloadHelper.class);

    public static final String EXTRA_WORKLOAD_DB = "WORKLOAD_DB";
    public static final String EXTRA_NODE_ID = "NODE_ID";
    public static final String EXTRA_MINIMUM_DELAY = "MINIMUM_DELAY";
    public static final String EXTRA_NUM_FRIENDS = "NUM_FRIENDS";

    public static final String DEFAULT_WORKLOAD_DB = "workload";

    public static final String DEFAULT_WORKLOAD_SYNC_URL = "http://mschoch.ic.ht/android";
    public static final String DEFAULT_LOGS_SYNC_URL = "http://mschoch.ic.ht/android-logs";

    public static final String TEST_RESULTS_DB = "test-results";

    /**
     * List of workload classes
     */
    private static List<String> getWorkloadClassNames() {
		//eventually replace this with something scanning the apk for a certain subpackage
		ArrayList<String> result = new ArrayList<String>();
		result.add("com.couchbase.androidtester.workloads.impl.CRUDDocuments");
		result.add("com.couchbase.workloads.impl.PhotoShare");
		result.add("com.couchbase.workloads.impl.Calendar");
		result.add("com.couchbase.workloads.impl.ContinuousReplication");
		result.add("com.couchbase.workloads.impl.FiveMinuteIntervalReplication");
		result.add("com.couchbase.workloads.impl.PushLogsReplication");
		return result;
    }

    /**
     * Return a list of instantiated CouchbaseWorkload objects
     * @return the list
     */
    public static ArrayList<CouchbaseWorkload> loadWorkloads() {
    	ArrayList<CouchbaseWorkload> workloads = new ArrayList<CouchbaseWorkload>();

    	List<String> allWorkloadClasses = getWorkloadClassNames();
    	for (String workloadClassName : allWorkloadClasses) {
			try {
			    CouchbaseWorkload cw = loadWorkload(workloadClassName);
				workloads.add(cw);
			} catch (Exception e) {
				LOG.error("Exception loading monitors", e);
			}
		}

    	return workloads;
    }

    public static CouchbaseWorkload loadWorkload(String name) throws ClassNotFoundException, InstantiationException, IllegalAccessException {
        Class<?> workloadClass = WorkloadHelper.class.getClassLoader().loadClass(name);
        CouchbaseWorkload cw = (CouchbaseWorkload)workloadClass.newInstance();
        return cw;
    }

}
