package de.swiftbird.elasticandroid;

import android.content.Context;
import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class AppLog {
    private static SelfLogCompBuffer logDao;

    public static int i(String tag, String msg) {
        insertLog("INFO", tag, msg);
        return Log.i(tag, msg);
    }

    public static int w(String tag, String msg) {
        insertLog("WARN", tag, msg);
        return Log.w(tag, msg);
    }

    public static int e(String tag, String msg) {
        insertLog("ERROR", tag, msg);
        return Log.e(tag, msg);
    }

    public static int e(String tag, String msg, Throwable tr) {
        insertLog("ERROR", tag, msg + '\n' + Log.getStackTraceString(tr));
        return Log.e(tag, msg);
    }

    public static int d(String tag, String msg) {
        insertLog("DEBUG", tag, msg);
        return Log.d(tag, msg);
    }

    // Add similar methods for d, v, w, etc., as needed

    private static void insertLog(String level, String tag, String message) {
            // Execute on a background thread to avoid blocking the main thread
            new Thread(() -> {
                try {
                    AppDatabase db = AppDatabase.getDatabase(AppInstance.getAppContext(), "");
                    FleetEnrollData enrollmentData = db.enrollmentDataDAO().getEnrollmentInfoSync(1);
                    PolicyData policyData = db.policyDataDAO().getPolicyDataSync();

                    SelfLogCompDocument document = new SelfLogCompDocument(enrollmentData, policyData, level, tag, message);
                    SelfLogComp selfLogComp = SelfLogComp.getInstance();
                    selfLogComp.setup(AppInstance.getAppContext() , null, null);
                    selfLogComp.addDocumentToBuffer(document);
                } catch (Exception e) {
                    // Ignore any exceptions, as it may be that the agent is not enrolled yet and therefor can't send logs anyway
                    return;
                }
            }).start();
    }
}
