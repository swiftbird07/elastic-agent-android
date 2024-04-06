package de.swiftbird.elasticandroid;

import android.util.Log;

/**
 * Provides a unified logging interface that extends Android's Log class functionalities.
 * In addition to outputting logs to the console, it inserts log records into the application's
 * database for persistence and further processing, depending on configured policies.
 */
public class AppLog {
    private static SelfLogCompBuffer logDao; // DAO for logging component, used for inserting logs into the database.

    /**
     * Logs an informational message both to the console and the application's log storage.
     *
     * @param tag   Tag used to identify the source of the log message.
     * @param msg   The message to log.
     * @return      The result from Android's native Log.i method.
     */
    public static int i(String tag, String msg) {
        insertLog("INFO", tag, msg);
        return Log.i(tag, msg);
    }

    /**
     * Logs a warning message both to the console and the application's log storage.
     *
     * @param tag   Tag for the log message.
     * @param msg   Warning message to log.
     * @return      The result from the native Log.w method.
     */
    public static int w(String tag, String msg) {
        insertLog("WARN", tag, msg);
        return Log.w(tag, msg);
    }

    /**
     * Logs an error message both to the console and the application's log storage.
     *
     * @param tag   Tag for the log message.
     * @param msg   Error message to log.
     * @return      The result from the native Log.e method.
     */
    public static int e(String tag, String msg) {
        insertLog("ERROR", tag, msg);
        return Log.e(tag, msg);
    }

    /**
     * Logs an error message along with an exception both to the console and the application's log storage.
     *
     * @param tag   Tag for the log message.
     * @param msg   Error message to log.
     * @param tr    Throwable associated with the error condition.
     * @return      The result from the native Log.e method.
     */
    public static int e(String tag, String msg, Throwable tr) {
        insertLog("ERROR", tag, msg + '\n' + Log.getStackTraceString(tr));
        return Log.e(tag, msg, tr);
    }

    /**
     * Logs a debug message both to the console and the application's log storage.
     *
     * @param tag   Tag for the log message.
     * @param msg   Debug message to log.
     * @return      The result from the native Log.d method.
     */
    public static int d(String tag, String msg) {
        insertLog("DEBUG", tag, msg);
        return Log.d(tag, msg);
    }

    /**
     * Inserts a log record into the application's log storage for persistence and later processing.
     * This operation is performed asynchronously to prevent blocking the main thread.
     *
     * @param level The severity level of the log message (e.g., INFO, WARN, ERROR).
     * @param tag   Tag associated with the log message.
     * @param message The log message.
     */
    private static void insertLog(String level, String tag, String message) {
            // Execute on a background thread to avoid blocking the main thread
            new Thread(() -> {
                try {
                    AppDatabase db = AppDatabase.getDatabase(AppInstance.getAppContext(), "");
                    FleetEnrollData enrollmentData = db.enrollmentDataDAO().getEnrollmentInfoSync(1);
                    PolicyData policyData = db.policyDataDAO().getPolicyDataSync();
                    boolean selfLogEnabled = policyData != null && policyData.paths != null && policyData.paths.contains("android://self-log");
                    if(!selfLogEnabled) {
                        return;
                    }

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
                                if ((level.equals("DEBUG") || level.equals("INFO")) && pathParts[1].equals("warn")) {
                                    return;
                                }
                                if ((level.equals("DEBUG") || level.equals("INFO") || level.equals("WARN")) && pathParts[1].equals("error")) {
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
