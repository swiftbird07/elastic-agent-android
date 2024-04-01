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
            SelfLogCompDocument document = new SelfLogCompDocument();
            document.logLevel = level;
            document.tag = tag;
            document.message = message;
            // Assuming ElasticsearchDocument has a timestamp field; set it if not auto-generated
            document.timestamp = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).format(new Date());

            SelfLogComp selfLogComp = SelfLogComp.getInstance();
            selfLogComp.addDocumentToBuffer(document);
        }).start();
    }
}
