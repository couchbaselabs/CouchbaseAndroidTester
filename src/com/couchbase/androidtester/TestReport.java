package com.couchbase.androidtester;

import java.util.HashMap;
import java.util.Map;

import android.content.Context;
import android.provider.Settings.Secure;
import android.telephony.TelephonyManager;

public class TestReport {

    private static String couchbaseVersion = "unknown";

    public static void setCouchbaseVersion(String couchbaseVersion) {
        TestReport.couchbaseVersion = couchbaseVersion;
    }

    public static Map<String, Object> newTestReport(Context context) {
        Map<String, Object> result = new HashMap<String, Object>();

        result.put("identifiers", getIdInfo(context));
        result.put("device", getDeviceInfo());
        result.put("os", getOsInfo());
        result.put("application", getApplicationInfo());

        return result;
    }

    public static Map<String, Object> getIdInfo(Context context) {
        Map<String, Object> result = new HashMap<String, Object>();

        final TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        result.put("telephony_id", tm.getDeviceId());
        result.put("sim_serial", tm.getSimSerialNumber());
        result.put("secure_id", Secure.getString(context.getContentResolver(), Secure.ANDROID_ID));

        return result;
    }

    public static Map<String, Object> getApplicationInfo() {
        Map<String, Object> result = new HashMap<String, Object>();

        result.put("debug", android.os.Debug.isDebuggerConnected());
        result.put("couchbase", couchbaseVersion);

        return result;
    }

    public static Map<String, Object> getDeviceInfo() {
        Map<String, Object> result = new HashMap<String, Object>();

        result.put("board", android.os.Build.BOARD);
        result.put("brand", android.os.Build.BRAND);
        result.put("device", android.os.Build.DEVICE);
        result.put("model", android.os.Build.MODEL);
        result.put("product", android.os.Build.PRODUCT);
        result.put("manufacturer", android.os.Build.MANUFACTURER);
        result.put("tags", android.os.Build.TAGS);
        result.put("cpu_abi", android.os.Build.CPU_ABI);

        return result;
    }

    public static Map<String, Object> getOsInfo() {
        Map<String, Object> result = new HashMap<String, Object>();

        result.put("release", android.os.Build.VERSION.RELEASE);
        result.put("incremental", android.os.Build.VERSION.INCREMENTAL);
        result.put("display", android.os.Build.DISPLAY);
        result.put("fingerprint", android.os.Build.FINGERPRINT);
        result.put("build_id", android.os.Build.ID);
        result.put("build_time", android.os.Build.TIME);
        result.put("build_type", android.os.Build.TYPE);
        result.put("build_user", android.os.Build.USER);

        return result;
    }

    public static Map<String, Object> getTestReport(String name, String systemName, long start, long end) {
        Map<String, Object> result = new HashMap<String, Object>();
        result.put("name", name);
        result.put("systemName", systemName);
        result.put("start", start);
        result.put("end", end);
        result.put("duration", end - start);
        return result;
    }

    public static Map<String, Object> createTestReport(Context context, String name, String systemName, long start, long end) {
        Map<String,Object> result = newTestReport(context);

        result.put("report", getTestReport(name, systemName, start, end));

        return result;
    }

}
