package de.swiftbird.elasticandroid;


import android.app.admin.DeviceAdminReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;

import java.util.Objects;

/**
 * Extends DeviceAdminReceiver to handle specific admin events related to the application's device management features.
 * This receiver is crucial for responding to security and network log availability, among other device admin events.
 */
public class AppDeviceAdminReceiver extends DeviceAdminReceiver {

    /**
     * Called when the application is granted device admin access.
     * This method can be used to perform initial setup actions or to enable specific features upon admin activation.
     * Currently this does nothing.
     *
     * @param context The Context in which the receiver is running.
     * @param intent  The Intent being received.
     */
    @Override
    public void onEnabled(@NonNull Context context, @NonNull Intent intent) {
        super.onEnabled(context, intent);
    }

    /**
     * Invoked when security logs are available for the device. This method initiates a new thread
     * to handle the security logs asynchronously to avoid blocking the main thread.
     *
     * @param context The Context in which the receiver is running.
     * @param intent  The Intent being received.
     */
    @Override
    public void onSecurityLogsAvailable(@NonNull Context context, @NonNull Intent intent) {
        super.onSecurityLogsAvailable(context, intent);
        AppLog.i("AppDeviceAdminReceiver", "Security logs available");
        SecurityLogsComp logsComp = SecurityLogsComp.getInstance();
        // New thread to handle the security logs
        new Thread(() -> logsComp.handleSecurityLogs(context)).start();
    }

    /**
     * Called when network logs are available. This method starts a new thread to process the network logs
     * asynchronously, based on the received batchToken and the count of network logs.
     *
     * @param context The Context in which the receiver is running.
     * @param intent  The Intent being received.
     * @param batchToken The token identifying the batch of network logs available.
     * @param networkLogsCount The number of network logs available in this batch.
     */
    @Override
    public void onNetworkLogsAvailable(@NonNull Context context, @NonNull Intent intent, long batchToken, int networkLogsCount) {
        super.onNetworkLogsAvailable(context, intent, batchToken, networkLogsCount);
        AppLog.i("AppDeviceAdminReceiver", "Network logs available from batchToken: " + batchToken + " with count: " + networkLogsCount);
        NetworkLogsComp logsComp = NetworkLogsComp.getInstance();
        // New thread to handle the network logs
        new Thread(() -> logsComp.handleNetworkLogs(context, batchToken)).start();
    }
}
