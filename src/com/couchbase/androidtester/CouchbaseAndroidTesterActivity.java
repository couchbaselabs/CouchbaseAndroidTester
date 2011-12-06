package com.couchbase.androidtester;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.ektorp.CouchDbConnector;
import org.ektorp.CouchDbInstance;
import org.ektorp.DbAccessException;
import org.ektorp.ReplicationCommand;
import org.ektorp.android.http.AndroidHttpClient;
import org.ektorp.android.util.EktorpAsyncTask;
import org.ektorp.http.HttpClient;
import org.ektorp.impl.StdCouchDbInstance;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ExpandableListView;
import android.widget.ListView;
import android.widget.TabHost;

import com.couchbase.android.CouchbaseMobile;
import com.couchbase.android.ICouchbaseDelegate;
import com.couchbase.androidtester.monitors.CouchbaseMonitor;
import com.couchbase.androidtester.monitors.MonitorHelper;
import com.couchbase.androidtester.widget.MonitorsListAdapter;
import com.couchbase.androidtester.widget.WorkloadsListAdapter;
import com.couchbase.androidtester.workloads.CouchbaseWorkload;
import com.couchbase.androidtester.workloads.WorkloadHelper;

public class CouchbaseAndroidTesterActivity extends Activity {

	public static final String TAG = "CouchbaseTester";

	public static final String DEFAULT_WORKLOAD_DB = "workload";
	public static final String DEFAULT_REPORT_DB = "reports";

	/**
	 * List of monitors
	 */
	List<CouchbaseMonitor> monitors;

	/**
	 * Expandable List View Adapter for monitors
	 */
	private MonitorsListAdapter monitorsListAdapter;

	/**
	 * Expandable List View for monitors
	 */
	private ExpandableListView monitorsListView;

	/**
	 * List of workloads
	 */
	private List<CouchbaseWorkload> workloads;

	/**
	 * List View Adapter for workloads
	 */
	private WorkloadsListAdapter workloadsListAdapter;

	/**
	 * List View for workloads
	 */
	private ListView workloadsListView;

	/**
	 * ServiceConnection reference to Couch
	 */
	private ServiceConnection couchServiceConnection;

	/**
	 * CouchDbIntsance of the embedded Couch
	 */
	private CouchDbInstance couchDbInstance;

	/**
	 * CouchDbConnector of the default database
	 */
	private CouchDbConnector workloadConnector;

	/**
     * CouchDbConnector of the default database
     */
    private CouchDbConnector reportConnector;

    /**
     * Track when we started Couchbase
     */
    private long couchStartTime;

	/**
	 * Startup Progress Dialog
	 */
	private ProgressDialog startupDialog;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        Resources res = getResources();

        TabHost tabs = (TabHost)findViewById(R.id.tabhost);

        tabs.setup();

        TabHost.TabSpec spec1 = tabs.newTabSpec("tag1");
        spec1.setContent(R.id.tab1);
        spec1.setIndicator("Monitors", res.getDrawable(R.drawable.tab_monitor));
        tabs.addTab(spec1);

        TabHost.TabSpec spec2 = tabs.newTabSpec("tag2");
        spec2.setContent(R.id.tab2);
        spec2.setIndicator("Workloads", res.getDrawable(R.drawable.tab_workload));
        tabs.addTab(spec2);

        //now load the rest of the UI

        monitors = MonitorHelper.loadMonitors();
        for (CouchbaseMonitor monitor : monitors) {
            monitor.setContext(this);
        }

        monitorsListAdapter = new MonitorsListAdapter(this, monitors);
        monitorsListView = (ExpandableListView)findViewById(R.id.monitorListView);
        monitorsListView.setAdapter(monitorsListAdapter);

        //default all monitor groups to be expanded
        for(int i=0; i < monitorsListAdapter.getGroupCount(); i++) {
            monitorsListView.expandGroup(i);
        }

        workloads = WorkloadHelper.loadWorkloads();

        workloadsListAdapter = new WorkloadsListAdapter(this, workloads);
        workloadsListView = (ListView)findViewById(R.id.workloadListView);
        workloadsListView.setAdapter(workloadsListAdapter);

        //check to see if debugger is attached and warn user
        if(android.os.Debug.isDebuggerConnected()) {
            AlertDialog alertDialog = new AlertDialog.Builder(this).create();
            alertDialog.setTitle("Debugger Attached");
            alertDialog.setMessage("The application has detected that a debugger is attached.  This may affect the performance numbers calculated by this program.");
            alertDialog.setButton("Ignore", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {

                startupDialog = new ProgressDialog(CouchbaseAndroidTesterActivity.this);
                startupDialog.setCancelable(false);
                startupDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                startupDialog.setTitle("Starting Couchbase");
                startupDialog.show();

                //proceed to start couch
                startCouch();
            }});
            alertDialog.setButton2("Quit", new DialogInterface.OnClickListener() {
              public void onClick(DialogInterface dialog, int which) {
                finish();
            }});
            alertDialog.show();
        }
        else {
            startupDialog = new ProgressDialog(CouchbaseAndroidTesterActivity.this);
            startupDialog.setCancelable(false);
            startupDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            startupDialog.setTitle("Starting Couchbase");
            startupDialog.show();

            //proceed to start couch
            startCouch();
        }

    }

    @Override
    protected void onDestroy() {
        if(couchServiceConnection != null) {
            unbindService(couchServiceConnection);
        }
    	super.onDestroy();
    }

	protected void startCouch() {
		CouchbaseMobile couch = new CouchbaseMobile(getBaseContext(), mDelegate);
		String couchbaseVersion = couch.getVersion();
		Log.v(TAG, "Couchbase Mobile version: " + couchbaseVersion);
		TestReport.setCouchbaseVersion(couchbaseVersion);
		couchStartTime = System.currentTimeMillis();
		couchServiceConnection = couch.startCouchbase();
	}

	private final ICouchbaseDelegate mDelegate = new ICouchbaseDelegate() {
		@Override
		public void couchbaseStarted(String host, int port) {
		    final long couchStartFinishTime = System.currentTimeMillis();

			Log.v(TAG, "Couchbase has started");
			HttpClient httpClient = new AndroidHttpClient.Builder().host(host).port(port).socketTimeout(0).build();
			couchDbInstance = new StdCouchDbInstance(httpClient);

			//now create a default database for workloads to use
			EktorpAsyncTask createDbs = new EktorpAsyncTask() {

                @Override
                protected void doInBackground() {
                    workloadConnector = couchDbInstance.createConnector(DEFAULT_WORKLOAD_DB, true);
                    reportConnector = couchDbInstance.createConnector(DEFAULT_REPORT_DB, true);
                }

                @Override
                protected void onDbAccessException(
                        DbAccessException dbAccessException) {
                    Log.e(TAG, "Error Creating Database", dbAccessException);
                }

                @Override
                protected void onSuccess() {
                    //record the couch start time report
                    Map<String,Object> couchStartTimeReport = TestReport.createTestReport(CouchbaseAndroidTesterActivity.this, "Couchbase Mobile Start Time", "couchbaseMobileStartTime", couchStartTime, couchStartFinishTime);
                    reportConnector.create(couchStartTimeReport);

                    //start a continuous push replication of test results to the cloud
                    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
                    ReplicationCommand pushReplicationCommand = new ReplicationCommand.Builder()
                        .source(DEFAULT_REPORT_DB)
                        .target(prefs.getString("report_sync_url", "http://couchdb2.couchbaseqe.com:5984/android-results"))
                        .continuous(true)
                        .build();
                    couchDbInstance.replicate(pushReplicationCommand);

                    //iterate through all the workloads and give them reference to Couch
                    for (CouchbaseWorkload workload : workloads) {
                        workload.setCouchDbInstance(couchDbInstance);
                        workload.setCouchDbConnector(workloadConnector);
                        workload.setReportConnector(reportConnector);
                        workload.setContext(CouchbaseAndroidTesterActivity.this);
                    }

                    //remove the progress dialog
                    if(startupDialog != null) {
                        startupDialog.hide();
                    }

                    //see if we were requested to start any workloads
                    Intent intent = getIntent();
                    if(intent != null) {
                        String startWorkloadString = intent.getStringExtra("WORKLOAD");
                        if(startWorkloadString != null) {
                            Log.d(TAG, "Requested to start workload " + startWorkloadString);
                            List<String> startWorkloads = Arrays.asList(startWorkloadString.split(","));

                            for (CouchbaseWorkload workload : workloads) {
                                if(startWorkloads.contains(workload.getClass().getName())) {
                                    Log.d(TAG, "Starting workload " + workload.getName());
                                    workload.start();
                                }
                            }
                        }
                    }
                }
            };

            createDbs.execute();

		};

		@Override
		public void exit(String error) {
		}
	};

	/**
	 * Register the About menu item
	 */
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(Menu.NONE, 0, 0, "About");
        return super.onCreateOptionsMenu(menu);
    }

    /**
     * Show the About dialog
     */
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case 0:
            	AlertDialog.Builder builder = new AlertDialog.Builder(this);
            	builder.setTitle("About CouchbaseAndroidTester")
            		   .setMessage("Icons by http://glyphish.com/ used under a Creative Commons Attribution license.")
            	       .setCancelable(true);
            	AlertDialog alert = builder.create();
            	alert.show();
                return true;
        }
        return false;
    }

}