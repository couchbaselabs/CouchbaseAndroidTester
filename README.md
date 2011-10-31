# Couchbase Android Tester

## Getting Started

This project requires the latest version of Couchbase Mobile for Android.  This is the only
dependency not included in the project source tree.  Please follow the instructions from
http://www.couchbase.org/get/couchbase-mobile-for-android/current to get and install the latest
version of Couchbase Mobile for Android.

## Building

By default Eclipse with the Android ADT Plugin installed will build both the Android and Java tester classes.

To build the Android tester application from the command-line:

    ant -Dsdk.dir=<android.sdk.path> debug

To build the Java tester application from the command-line:

    ant -f java_build.xml compile

To clean build artifacts:

    ant -Dsdk.dir=<android.sdk.path> clean
    ant -f java_build.xml clean

## Running workloads from an Android device through the UI

The simplest way to start workloads is by pressing the Start button from the workloads tab in the UI.

## Running workloads from an Android device from the command-line

    adb -e shell am start -a android.intent.action.MAIN -n com.couchbase.androidtester/.CouchbaseAndroidTesterActivity -e WORKLOAD com.couchbase.workloads.impl.CRUDDocuments

### Arguments

-e WORKLOAD &lt;comma-delimited list of workloads&gt;
 
-e WORKLOAD_SYNC_URL &lt;URL to which the workload DB will be synced&gt; 
 
-e LOGS_SYNC_URL &lt;URL to which logs will be pushed&gt;

## Running workloads with Java from the command-line

    java -cp bin:libs/org.ektorp-1.2.2-SNAPSHOT.jar:libs/slf4j-api-1.6.1.jar:libs/slf4j-jdk14-1.6.1.jar:javalibs/httpclient-4.1.1.jar:javalibs/httpcore-4.1.jar:javalibs/commons-logging-1.1.1.jar:javalibs/httpclient-cache-4.1.1.jar:libs/jackson-core-asl-1.8.5.jar:libs/jackson-mapper-asl-1.8.5.jar:libs/commons-io-2.0.1.jar com.couchbase.javatester.JavaTester -workload com.couchbase.workloads.impl.PhotoShare,com.couchbase.workloads.impl.FiveMinuteIntervalReplication < couch_urls.txt
    
### Arguments

-workload &lt;comma-delimited list of workloads&gt;

-workload_sync_url &lt;URL to which the workload DB will be synced&gt;

-log_sync_url &lt;URL to which logs will be pushed&gt;

-min_delay &lt;delay in ms, default 500&gt;

-num_friends &lt;number of friends to tag in document, default 2&gt;
    
NOTE: the single command-line argument accepted is a comma-delimited list of workloads to be run.  A list of CouchDB server URLs must be provided on standard input, 1 URL per line (see the couch_urls.txt file for an example of the format)

### Output

<pre>
Workload Sync URL: http://127.0.0.1:5984/android-other
Minimum Delay: 100
Number of Friends: 5
Starting Workloads: com.couchbase.workloads.impl.Calendar,com.couchbase.workloads.impl.ContinuousFriendFilterReplication
1a1758da-c69a-4767-bde3-5ca16df832a8,69,1321,710,288,22,588
1ee343e8-c7a4-40f4-96c3-eeacfbe45aa4,72,1535,1757,280,600,16
76d288d5-d7af-4680-aa97-b0f79cd68359,168,10,1127,507,350,401
8d912e56-e157-46fd-b722-c80df1d10a5e,750,1629,1913,1070,45,857
9f5df4dc-004f-4592-b6b4-5f387b666022,62,1470,436,143,19,294
129ea10c-e651-4dbf-b860-de019aa19432,974,26,1884,2179,1278,1115
421b2dd8-c711-47b0-ae27-29cd6caf5ef7,322,1230,1853,644,28,454
</pre>

Any of the settings overridden on the command-line and the list of workloads that will be started are sent to standard output.

Following that the output is comma-delimited with the following fields:
&lt;document id&gt;, &lt;sync time to cloud&gt;, &lt;sync time to friend 1&gt; ... &lt;sync time to friend n&gt;

The number of columns will be 2 + the number of friends tagged in each document.

## Provided Workloads

- CRUD Documents  -  Create, Read, Update and Delete documents in sequence

- Photo Share  -  Create Documents and Attach Photos

- Calendar Usage  -  Create and Update Calendar Events

- Continuous Replication  -  Continuous bi-directional replication of the workload database to the cloud

- Five Minute Interval Replication  -  Non-continuous replication of the workload database at 5 minute intervals

- ContinuousFriendFilterReplication - Continuous replication of all documents from device to cloud, from cloud to device replication is filtered to only include friends tagged in document

- FiveMinuteIntervalFriendFilterReplication - Non-continous replication of the workload database at 5 minute intervals, all documents replicated from device to cloud, from cloud to device replication is filtered to only include friends tagged in document 

## Provided Monitors

- Battery Level  -  Records the current battery level and plug status

- Couchbase  -  Records the status of Couchbase and the host/port

- Memory  -  Records various memory statistics provided by the Android platform

- Network  -  Records the network status and network interface type

## Adding your own workloads

Workloads are built by creating a class that extends the CouchbaseWorkload class.  The
CouchbaseWorkload class is abstract and requires you implement one method performWork().  In
performWork() you are to perform the entire workload.

### Registering the workload class

To have your workload class show up in the UI, you must register it in WorkloadHelper.java.

### Customizing the name displayed in the UI

To customize the name displayed in the UI, override the getDisplayName() method of CouchbaseWorkload.

### Making sure its possible to stop the workload

To ensure your workload can be stopped when requested by the user, your workload should
periodically check the status of thread.isCancelled().  If this returns true, your performWork()
method should return as soon as possible.

## Adding your own performance monitors

Performance monitors are built by implementing the CouchbaseMonitor interface.  Two convenience
classes are available depending on the type of monitor you are building.  You can extend
CouchbasePassiveMonitor for building a monitor that can operate without a thread of execution.
Or you can extend CouchbasePollingMonitor for building a monitor that will periodically poll
conditions.

It is important to note that the architecture is such that a single performance monitor can
return multiple measures.  This is useful for supporting operations where polling a single
API periodically is expensive, but yields multiple measures.  In this case a single Monitor
implementation should manage all the measures (see the Memory Monitor example).

### Setting up the monitor

If the monitor requires any set up, the start() method can be overridden.

### Passive Monitors

Passive Monitors should call monitorDisplay.valueChanged() whenever they have determined that
the value has changed.  Then they simply must respond to the currentMeasures() method returning 
a List of Strings describing the current measures.  (see the Battery Level Monitor example)

### Polling Monitors

Polling Monitors should implement getMonitorValues() which will be called periodically to poll
the system for the measures supported by this monitor.  (see the Memory Monitor example)

### Registering the monitor class

To have your monitor class show up in the UI, you must register it in MonitorHelper.java.

## Attribution

This project uses icons from http://glyphish.com/ under the Creative Commons Attribution license.
