package de.swiftbird.elasticandroid;

import static androidx.core.content.ContextCompat.getSystemService;

import android.app.admin.DevicePolicyManager;
import android.app.admin.SecurityLog;
import android.content.ComponentName;
import android.content.Context;
import android.os.Build;
import android.util.Log; // We don't use AppLog for non-warnings/errors because this would double-log the messages that are sent anyway

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Optional component that manages the security log events, leveraging the Device Policy Manager to enable and access security logs.
 * This component is integral to monitoring, auditing, and analyzing security-related events that occur on the device,
 * aiding in identifying potential security threats or policy violations.
 *
 * <p>It relies on {@link AppDeviceAdminReceiver} to activate security logging and fetch security log entries. The fetched logs
 * are processed and stored in a local database, enabling persistent storage and analysis of security events over time.</p>
 *
 * <p>Activation of this component requires the app to be designated as a device owner, a status achieved during the initial
 * device setup. This elevated status grants the app the ability to access sensitive logs and perform system-level operations
 * related to security.</p>
 *
 * <p>Important: The capability to enable security logging and retrieve logs is available only on devices running
 * Android Oreo (API level 26) or newer. This reflects the Android platform's evolving approach to security and device management.</p>
 *
 * <p>This component plays a critical role in maintaining device security, offering insights into the device's operational
 * integrity and the actions of users and apps that could impact security posture.</p>
 *
 * <p>For missing documentation, refer to the Component interface.
 */
public class SecurityLogsComp implements Component {

    private static final String TAG = "SecurityLogsComp";

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

    /**
     * Processes available security log events. This method retrieves and processes security log events from the
     * {@link AppDeviceAdminReceiver}, converting them into a format suitable for storage and/or transmission.
     *
     * @param context The application context.
     */
    public void handleSecurityLogs(Context context) {
        // First setup the component
        AppDatabase db = AppDatabase.getDatabase(context, "");
        this.buffer = db.securityLogCompBuffer();
        this.statistic = db.statisticsDataDAO();

        AppLog.i(TAG, "Received callback for security logs available.");
        ComponentName adminComponent = new ComponentName(context, AppDeviceAdminReceiver.class);
        DevicePolicyManager dpm = (DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE);
        try {
            if (dpm.isSecurityLoggingEnabled(adminComponent)) {
                List<SecurityLog.SecurityEvent> logs = dpm.retrieveSecurityLogs(adminComponent);
                if (logs != null) {
                    for (SecurityLog.SecurityEvent event : logs) {
                        // Process each security log event
                        Log.d(TAG, "Security log event: " + event.toString());

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
                    AppLog.w(TAG, "No security logs were available, even though the callback was received.");
                }
            } else {
                Log.d(TAG, "Security logging not enabled.");
            }
        } catch (Exception e) {
            AppLog.e(TAG, "Failed to retrieve security logs: " + Arrays.toString(e.getStackTrace()));
        }

    }

    /**
     * Sets up the security logs component for collecting security events. This method initializes the component,
     * enabling security logging through the {@link AppDeviceAdminReceiver} and setting necessary configurations.
     *
     * @param context The application context.
     * @param enrollmentData Data regarding the device's enrollment status.
     * @param policyData Policy data affecting how security logs are handled.
     * @param subComponent A string identifier for the sub-component, not used in this context.
     * @return {@code true} if setup was successful and security logging is supported and enabled; otherwise, {@code false}.
     */
    @Override
    public boolean setup(Context context, FleetEnrollData enrollmentData, PolicyData policyData, String subComponent) {
        // Initialize Room database and get the DAO
        AppDatabase db = AppDatabase.getDatabase(context, "");
        buffer = db.securityLogCompBuffer();
        statistic = db.statisticsDataDAO();

        // Enable security logging
        AppLog.d(TAG, "Setting up security logs component");
        DevicePolicyManager dpm = (DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE);
        ComponentName adminComponentName = new ComponentName(context, AppDeviceAdminReceiver.class);
        try {
            if (dpm.isDeviceOwnerApp(context.getPackageName())) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    Set<String> affiliationIds = new HashSet<>();
                    affiliationIds.add("de.swiftbird.elasticandroid");
                    dpm.setAffiliationIds(adminComponentName, affiliationIds);
                }
                dpm.setSecurityLoggingEnabled(adminComponentName, true);

                AppLog.d(TAG, "Security logging enabled.");
                return true;
            } else {
                AppLog.w(TAG, "This app is not a device owner app, security logging can not be enabled.");
            }
        } catch (Exception e) {
            AppLog.e(TAG, "Unhandled when enabling security logging: " + e.getMessage());
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
            Log.w(TAG, "Invalid document type or buffer not initialized");
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

    /**
     * This method disables security logging through the {@link AppDeviceAdminReceiver}
     * and cleans up any resources used by the component.
     *
     * @param context The application context.
     * @param enrollmentData Data regarding the device's enrollment status.
     * @param policyData Policy data affecting how security logs are handled.
     */
    @Override
    public void disable(Context context, FleetEnrollData enrollmentData, PolicyData policyData) {
        AppLog.d(TAG, "Disabling security logs component");
        DevicePolicyManager dpm = (DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE);
        ComponentName adminComponentName = new ComponentName(context, AppDeviceAdminReceiver.class);
        dpm.setSecurityLoggingEnabled(adminComponentName, false);
    }

    /**
     * This method returns the name of the security event tag based on the tag ID.
     *
     * @param tag The tag ID of the security event.
     * @return The name of the security event tag.
     */
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

