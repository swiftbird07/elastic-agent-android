package de.swiftbird.elasticandroid;

import static androidx.core.content.ContextCompat.getSystemService;

import android.app.admin.DevicePolicyManager;
import android.app.admin.SecurityLog;
import android.content.ComponentName;
import android.content.Context;
import android.os.Build;
import android.util.Log;

import java.util.Arrays;
import java.util.List;

public class SecurityLogsComp implements Component {

    private SecurityLogsCompBuffer buffer;

    private AppStatisticsDataDAO statistic;
    private static SecurityLogsComp securityLogsComp;

    public static SecurityLogsComp getInstance() {
        // Singleton pattern
        if (securityLogsComp == null) {
            securityLogsComp = new SecurityLogsComp();
        }
        return securityLogsComp;
    }



    public void handleSecurityLogs(Context context) {
        AppLog.i("SecurityLogsComp", "handleSecurityLogs - Received callback for security logs available.");
        ComponentName adminComponent = new ComponentName(context, AppDeviceAdminReceiver.class);
        DevicePolicyManager dpm = (DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE);
        try {
            if (dpm.isSecurityLoggingEnabled(adminComponent)) {
                List<SecurityLog.SecurityEvent> logs = dpm.retrieveSecurityLogs(adminComponent);
                if (logs != null) {
                    for (SecurityLog.SecurityEvent event : logs) {
                        // Process each security log event
                        Log.i("SecurityLogsComp", "Security log event: " + event.toString());

                        AppDatabase db = AppDatabase.getDatabase(context, "enrollment-data");
                        FleetEnrollData enrollmentData = db.enrollmentDataDAO().getEnrollmentInfoSync(1);
                        PolicyData policyData = db.policyDataDAO().getPolicyDataSync();
                        int logLevel;

                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                            logLevel = event.getLogLevel();
                        } else {
                            logLevel = -1;
                        }

                        // Case statement to convert log level int to string
                        String logLevelName;
                        switch (logLevel) {
                            case SecurityLog.LEVEL_ERROR:
                                logLevelName = "ERROR";
                                break;
                            case SecurityLog.LEVEL_WARNING:
                                logLevelName = "WARNING";
                                break;
                            case SecurityLog.LEVEL_INFO:
                                logLevelName = "INFO";
                                break;
                            default:
                                logLevelName = "UNKNOWN";
                                break;
                        }

                        int tag = event.getTag();
                        String tagName = getSecurityEventTagName(tag);

                        String message = event.toString();

                        addDocumentToBuffer(new SecurityLogsCompDocument(enrollmentData, policyData, logLevelName, tagName, message));
                    }
                } else {
                    Log.d("SecurityLogsComp", "No security logs available.");
                }
            } else {
                Log.d("SecurityLogsComp", "Security logging not enabled.");
            }
        } catch (Exception e) {
            AppLog.e("SecurityLogsComp", "Failed to retrieve security logs: " + Arrays.toString(e.getStackTrace()));
        }

    }

    @Override
    public boolean setup(Context context, FleetEnrollData enrollmentData, PolicyData policyData, String subComponent) {
        // Initialize Room database and get the DAO
        AppDatabase db = AppDatabase.getDatabase(context, "");
        buffer = db.securityLogCompBuffer();
        statistic = db.statisticsDataDAO();

        // Enable security logging
        AppLog.d("SecurityLogsComp", "Setting up security logs component");
        DevicePolicyManager dpm = (DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE);
        ComponentName adminComponentName = new ComponentName(context, AppDeviceAdminReceiver.class);
        try {
            if (dpm.isDeviceOwnerApp(context.getPackageName())) {
                dpm.setSecurityLoggingEnabled(adminComponentName, true);
                AppLog.d("SecurityLogsComp", "Security logging enabled.");
                return true;
            } else {
                AppLog.w("SecurityLogsComp", "This app is not a device owner app, security logging can not be enabled.");
            }
        } catch (Exception e) {
            AppLog.e("SecurityLogsComp", "Failed to enable security logging: " + e.getMessage());
        }
        return false;
    }

    @Override
    public void collectEvents(FleetEnrollData enrollmentData, PolicyData policyData) {
        // No-op for this component (logs are collected in real-time)
    }

    @Override
    public void addDocumentToBuffer(ElasticDocument document) {
        if (document instanceof SecurityLogsCompDocument && buffer != null) {
            buffer.insertDocument((SecurityLogsCompDocument) document);
            statistic.increaseCombinedBufferSize(1);
        }
        else {
            Log.w("SelfLogComp", "Invalid document type or buffer not initialized");
        }
    }

    @Override
    public <T extends ElasticDocument> List<T> getDocumentsFromBuffer(int maxDocuments) {
        int toIndex = Math.min(maxDocuments, buffer.getDocumentCount());
        List<SecurityLogsCompDocument> logBuffer = buffer.getOldestDocuments(toIndex);
        buffer.deleteOldestDocuments(toIndex);

        @SuppressWarnings("unchecked") // Safe cast
        List<T> result = (List<T>) logBuffer;
        return result;
    }

    @Override
    public int getDocumentsInBufferCount() {return buffer.getDocumentCount();}

    @Override
    public List<String> getRequiredPermissions() {
        // This component does require Device Owner permissions, but these permissions can not be granted by the user anyway
        // so we don't need to return any permissions here
        return null;
    }

    @Override
    public String getPathName() {
        return "security-logs";
    }

    @Override
    public void disable(Context context, FleetEnrollData enrollmentData, PolicyData policyData) {
        /*
        AppLog.d("SecurityLogsComp", "Disabling security logs component");
        DevicePolicyManager dpm = (DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE);
        ComponentName adminComponentName = new ComponentName(context, AppDeviceAdminReceiver.class);
        dpm.setSecurityLoggingEnabled(adminComponentName, false);

         */
    }

    public String getSecurityEventTagName(int tag) {
        switch (tag) {
            case 210002:
                return "TAG_ADB_SHELL_CMD";
            case 210001:
                return "TAG_ADB_SHELL_INTERACTIVE";
            case 210005:
                return "TAG_APP_PROCESS_START";
            case 210039:
                return "TAG_BLUETOOTH_CONNECTION";
            case 210040:
                return "TAG_BLUETOOTH_DISCONNECTION";
            case 210034:
                return "TAG_CAMERA_POLICY_SET";
            case 210029:
                return "TAG_CERT_AUTHORITY_INSTALLED";
            case 210030:
                return "TAG_CERT_AUTHORITY_REMOVED";
            case 210033:
                return "TAG_CERT_VALIDATION_FAILURE";
            case 210031:
                return "TAG_CRYPTO_SELF_TEST_COMPLETED";
            case 210021:
                return "TAG_KEYGUARD_DISABLED_FEATURES_SET";
            case 210006:
                return "TAG_KEYGUARD_DISMISSED";
            case 210007:
                return "TAG_KEYGUARD_DISMISS_AUTH_ATTEMPT";
            case 210008:
                return "TAG_KEYGUARD_SECURED";
            case 210026:
                return "TAG_KEY_DESTRUCTION";
            case 210024:
                return "TAG_KEY_GENERATED";
            case 210025:
                return "TAG_KEY_IMPORT";
            case 210032:
                return "TAG_KEY_INTEGRITY_VIOLATION";
            case 210011:
                return "TAG_LOGGING_STARTED";
            case 210012:
                return "TAG_LOGGING_STOPPED";
            case 210015:
                return "TAG_LOG_BUFFER_SIZE_CRITICAL";
            case 210020:
                return "TAG_MAX_PASSWORD_ATTEMPTS_SET";
            case 210019:
                return "TAG_MAX_SCREEN_LOCK_TIMEOUT_SET";
            case 210013:
                return "TAG_MEDIA_MOUNT";
            case 210014:
                return "TAG_MEDIA_UNMOUNT";
            case 210010:
                return "TAG_OS_SHUTDOWN";
            case 210009:
                return "TAG_OS_STARTUP";
            case 210041:
                return "TAG_PACKAGE_INSTALLED";
            case 210043:
                return "TAG_PACKAGE_UNINSTALLED";
            case 210042:
                return "TAG_PACKAGE_UPDATED";
            case 210036:
                return "TAG_PASSWORD_CHANGED";
            case 210035:
                return "TAG_PASSWORD_COMPLEXITY_REQUIRED";
            case 210017:
                return "TAG_PASSWORD_COMPLEXITY_SET";
            case 210016:
                return "TAG_PASSWORD_EXPIRATION_SET";
            case 210018:
                return "TAG_PASSWORD_HISTORY_LENGTH_SET";
            case 210022:
                return "TAG_REMOTE_LOCK";
            case 210003:
                return "TAG_SYNC_RECV_FILE";
            case 210004:
                return "TAG_SYNC_SEND_FILE";
            case 210027:
                return "TAG_USER_RESTRICTION_ADDED";
            case 210028:
                return "TAG_USER_RESTRICTION_REMOVED";
            case 210037:
                return "TAG_WIFI_CONNECTION";
            case 210038:
                return "TAG_WIFI_DISCONNECTION";
            case 210023:
                return "TAG_WIPE_FAILURE";
            default:
                return "UNKNOWN_TAG";
        }
    }



}

