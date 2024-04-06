package de.swiftbird.elasticandroid;

import android.util.Log;

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

                    // Get the path from the policy data for self-log in the "," separated format
                    String[] paths = policyData.paths.split(",");
                    for (String path : paths) {
                        if (path.startsWith("android://self-log")) {
                            // The path is in the format "android://self-log.INFO"
                            String[] pathParts = path.split("\\.");
                            if (pathParts.length > 1) {
                                // Check if the log level is high enough to be sent
                                if (level.equals("DEBUG") && pathParts[1].equals("info")) {
                                    return;
                                }
                                if (level.equals("DEBUG") || level.equals("INFO") && pathParts[1].equals("warn")) {
                                    return;
                                }
                                if (level.equals("INFO") || level.equals("WARN") && pathParts[1].equals("error")) {
                                    return;
                                }
                            } else {
                                // If no log level is specified, send only INFO logs
                                if (!level.equals("INFO")) {
                                    return;
                                }
                            }
                        }
                    }


                    SelfLogCompDocument document = new SelfLogCompDocument(enrollmentData, policyData, level, tag, message);
                    SelfLogComp selfLogComp = SelfLogComp.getInstance();
                    selfLogComp.setup(AppInstance.getAppContext() , null, null, "");
                    selfLogComp.addDocumentToBuffer(document);
                } catch (Exception e) {
                    // Ignore any exceptions, as it may be that the agent is not enrolled yet and therefor can't send logs anyway
                    return;
                }
            }).start();
    }
}
