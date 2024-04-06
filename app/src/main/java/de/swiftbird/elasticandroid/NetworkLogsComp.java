package de.swiftbird.elasticandroid;

import android.app.admin.ConnectEvent;
import android.app.admin.DevicePolicyManager;
import android.app.admin.DnsEvent;
import android.app.admin.NetworkEvent;
import android.content.ComponentName;
import android.content.Context;
import android.os.Build;
import android.util.Log; // We don't use AppLog for non-warnings/errors because this would double-log the messages that are sent anyway

import androidx.annotation.Nullable;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class NetworkLogsComp implements Component {
    
    private static final String TAG = "NetworkLogsComp";

    private NetworkLogsCompBuffer buffer;

    private AppStatisticsDataDAO statistic;
    private static NetworkLogsComp NetworkLogsComp;

    public static NetworkLogsComp getInstance() {
        // Singleton pattern
        if (NetworkLogsComp == null) {
            NetworkLogsComp = new NetworkLogsComp();
        }
        return NetworkLogsComp;
    }

    public void handleNetworkLogs(Context context, long batchToken) {
        // First setup the component
        AppDatabase db = AppDatabase.getDatabase(context, "");
        this.buffer = db.networkLogsCompBuffer();
        this.statistic = db.statisticsDataDAO();

        AppLog.d("NetworkLogsComp", "Received callback for network logs available.");
        ComponentName adminComponent = new ComponentName(context, AppDeviceAdminReceiver.class);
        DevicePolicyManager dpm = (DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE);
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (dpm.isNetworkLoggingEnabled(adminComponent)) {
                    if(batchToken == 0) {
                        AppLog.w(TAG, "Batch token is 0, this should not happen.");
                        return;
                    }
                    List<NetworkEvent> logs = dpm.retrieveNetworkLogs(adminComponent, batchToken);
                    if (logs != null) {
                        for (NetworkEvent event : logs) {
                            // Process each network log event
                            Log.d(TAG, "Network log event: " + event.toString());
                            FleetEnrollData enrollmentData = db.enrollmentDataDAO().getEnrollmentInfoSync(1);
                            PolicyData policyData = db.policyDataDAO().getPolicyDataSync();

                            if (event instanceof DnsEvent) {
                                Log.d(TAG, "Creating DNS event: " + event.toString());
                                DnsEvent dnsEvent = (DnsEvent) event;
                                addDocumentToBuffer(new NetworkLogsCompDocument(enrollmentData, policyData, "DNS", dnsEvent.getPackageName(), dnsEvent.getHostname(), dnsEvent.getInetAddresses(), dnsEvent.toString()));
                            } else if (event instanceof ConnectEvent) {
                                Log.d(TAG, "Creating CONNECT event: " + event.toString());
                                ConnectEvent connectEvent = (ConnectEvent) event;
                                addDocumentToBuffer(new NetworkLogsCompDocument(enrollmentData, policyData, "CONNECT", connectEvent.getPackageName(), connectEvent.getInetAddress(), connectEvent.getPort(), connectEvent.toString()));
                            } else {
                                AppLog.w(TAG, "Unknown network log event type: " + event.getClass().getName());
                            }
                        }
                    } else {
                        AppLog.w(TAG, "No network logs were available, even though the callback was received.");
                    }
                } else {
                    Log.d(TAG, "Network logging not enabled.");
                }
            } else {
                Log.d(TAG, "Network logging not supported on this device.");
            }
        } catch (Exception e) {
            AppLog.e(TAG, "Failed to retrieve network logs: " + Arrays.toString(e.getStackTrace()));
        }

    }

    @Override
    public boolean setup(Context context, FleetEnrollData enrollmentData, PolicyData policyData, String subComponent) {
        // Initialize Room database and get the DAO
        AppDatabase db = AppDatabase.getDatabase(context, "");
        buffer = db.networkLogsCompBuffer();
        statistic = db.statisticsDataDAO();

        // Enable network logging
        AppLog.d(TAG, "Setting up network logs component");
        DevicePolicyManager dpm = (DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE);
        ComponentName adminComponentName = new ComponentName(context, AppDeviceAdminReceiver.class);
        try {
            if (dpm.isDeviceOwnerApp(context.getPackageName())) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    Set<String> affiliationIds = new HashSet<>();
                    affiliationIds.add("de.swiftbird.elasticandroid");
                    dpm.setAffiliationIds(adminComponentName, affiliationIds);

                    dpm.setNetworkLoggingEnabled(adminComponentName, true);

                    AppLog.d(TAG, "Network logging enabled.");
                    return true;
                } else {
                    AppLog.w(TAG, "Network logging not supported on this device.");
                }
            } else {
                AppLog.w(TAG, "This app is not a device owner app, network logging can not be enabled.");
            }
        } catch (Exception e) {
            AppLog.e(TAG, "Unhandled when enabling network logging: " + e.getMessage());
        }
        return false;
    }

    @Override
    public void collectEvents(FleetEnrollData enrollmentData, PolicyData policyData) {
        // No-op for this component (logs are collected in real-time)
    }

    @Override
    public void addDocumentToBuffer(ElasticDocument document) {
        if (document instanceof NetworkLogsCompDocument && buffer != null) {
            buffer.insertDocument((NetworkLogsCompDocument) document);
            statistic.increaseCombinedBufferSize(1);
        }
        else {
            Log.w(TAG, "Invalid document type or buffer not initialized");
        }
    }

    @Override
    public <T extends ElasticDocument> List<T> getDocumentsFromBuffer(int maxDocuments) {
        int toIndex = Math.min(maxDocuments, buffer.getDocumentCount());
        List<NetworkLogsCompDocument> logBuffer = buffer.getOldestDocuments(toIndex);
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
        return "network-logs";
    }

    @Override
    public void disable(Context context, FleetEnrollData enrollmentData, PolicyData policyData) {
        /*
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            AppLog.d(TAG, "Disabling network logs component");
            DevicePolicyManager dpm = (DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE);
            ComponentName adminComponentName = new ComponentName(context, AppDeviceAdminReceiver.class);
            dpm.setNetworkLoggingEnabled(adminComponentName, false);
        }

         */
    }

    
}


