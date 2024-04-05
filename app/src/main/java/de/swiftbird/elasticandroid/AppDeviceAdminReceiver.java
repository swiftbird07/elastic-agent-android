package de.swiftbird.elasticandroid;


import android.app.admin.DeviceAdminReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;

import java.util.Objects;

public class AppDeviceAdminReceiver extends DeviceAdminReceiver {
    @Override
    public void onEnabled(@NonNull Context context, @NonNull Intent intent) {
        super.onEnabled(context, intent);
        // Called when this application is granted device admin access.
    }

    @Override
    public void onSecurityLogsAvailable(@NonNull Context context, @NonNull Intent intent) {
        super.onSecurityLogsAvailable(context, intent);
        AppLog.i("AppDeviceAdminReceiver", "Security logs available");
        SecurityLogsComp logsComp = SecurityLogsComp.getInstance();
        // New thread to handle the security logs
        new Thread(() -> logsComp.handleSecurityLogs(context)).start();
    }

    @Override
    public void onNetworkLogsAvailable(@NonNull Context context, @NonNull Intent intent, long batchToken, int networkLogsCount) {
        super.onNetworkLogsAvailable(context, intent, batchToken, networkLogsCount);
        AppLog.i("AppDeviceAdminReceiver", "Network logs available from batchToken: " + batchToken + " with count: " + networkLogsCount);
        NetworkLogsComp logsComp = NetworkLogsComp.getInstance();
        // New thread to handle the network logs
        new Thread(() -> logsComp.handleNetworkLogs(context, batchToken)).start();
    }

/*
    @Override
    public void onReceive(@NonNull Context context, @NonNull Intent intent) {
        Log.i("AppDeviceAdminReceiver", "Received intent: " + intent.getAction());
        if(Objects.equals(intent.getAction(), DeviceAdminReceiver.ACTION_DEVICE_ADMIN_ENABLED)) {
            Log.d("AppDeviceAdminReceiver", "Device admin enabled");
        } else if(Objects.equals(intent.getAction(), DeviceAdminReceiver.ACTION_SECURITY_LOGS_AVAILABLE)) {
            Log.d("AppDeviceAdminReceiver", "Security logs available");
        } else if(Objects.equals(intent.getAction(), DeviceAdminReceiver.ACTION_NETWORK_LOGS_AVAILABLE)) {
            Log.d("AppDeviceAdminReceiver", "Network logs available");
            // We have to invoke our network logs component here, as for whatever reason, the onNetworkLogsAvailable method is not called
            NetworkLogsComp logsComp = NetworkLogsComp.getInstance();
            logsComp.handleNetworkLogs(context, 0);
        } else {
            super.onReceive(context, intent);
        }
    }

 */
}
