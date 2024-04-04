package de.swiftbird.elasticandroid;


import android.app.admin.DeviceAdminReceiver;
import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;

public class AppDeviceAdminReceiver extends DeviceAdminReceiver {
    @Override
    public void onEnabled(@NonNull Context context, @NonNull Intent intent) {
        super.onEnabled(context, intent);
        // Called when this application is granted device admin access.
    }

    @Override
    public void onSecurityLogsAvailable(@NonNull Context context, @NonNull Intent intent) {
        super.onSecurityLogsAvailable(context, intent);
        // Security logs are available. Handle them in SecurityLogsComp.
        SecurityLogsComp logsComp = SecurityLogsComp.getInstance();
        logsComp.handleSecurityLogs(context);
    }
}
