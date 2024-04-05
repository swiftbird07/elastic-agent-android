package de.swiftbird.elasticandroid;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class AppBroadcastReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            AppLog.i("AppBroadcastReceiver", "Device booted up");
            // Start the location component
            LocationReceiver locationReceiver = new LocationReceiver(context);
        }
    }
}