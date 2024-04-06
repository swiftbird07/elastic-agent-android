package de.swiftbird.elasticandroid;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * Listens for system-wide broadcast events that the app is interested in.
 * Specifically, this receiver is set up to listen for the device's boot completion event.
 * Upon receiving this event, it initiates the location receiver component to start its operations.
 */
public class AppBroadcastReceiver extends BroadcastReceiver {

    /**
     * Called when the receiver receives a broadcast that it is registered for.
     * This implementation checks if the broadcast received is for the boot completion of the device.
     * If so, it logs the event and initializes the location receiver component.
     *
     * @param context The Context in which the receiver is running.
     * @param intent  The Intent being received, containing the broadcast action.
     */
    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            AppLog.i("AppBroadcastReceiver", "Device booted up");
            // Initialize the location receiver to start its operation post-boot.
            LocationReceiver locationReceiver = new LocationReceiver(context);
        }
    }
}
