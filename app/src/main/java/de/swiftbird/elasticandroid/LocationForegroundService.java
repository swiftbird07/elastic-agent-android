package de.swiftbird.elasticandroid;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.LocationManager;
import android.os.IBinder;
import androidx.core.app.NotificationCompat;

/**
 * A foreground service designed to continuously track location updates in the background.
 * This service leverages the {@link LocationManager} to request periodic location updates.
 * It ensures the app remains alive and can perform operations even when the app is in the background.
 * <p>
 * Upon starting, the service moves itself to the foreground state with a persistent notification,
 * which is a requirement from Android Oreo onwards to allow background location tracking.
 */
public class LocationForegroundService extends Service {
    // Channel ID for the notification
    private static final String CHANNEL_ID = "location_service";

    // Default values for requesting location updates (in case they are not provided)
    private static final long MIN_TIME_MS = 30000; // 30 seconds
    private static final float MIN_DISTANCE_METERS = 10; // 10 meters

    private LocationManager locationManager;

    @Override
    public void onCreate() {
        super.onCreate();
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
    }

    /**
     * Starts the service in the foreground and requests location updates.
     * Parameters for requesting location updates (e.g., minimum time interval, minimum distance,
     * location provider) are extracted from the Intent passed to this method.
     *
     * @param intent The Intent supplied to {@link Context#startService}, containing the configuration for location updates.
     * @param flags Additional data about this start request.
     * @param startId A unique integer representing this specific request to start.
     * @return The return value indicates what semantics the system should use for the service's current started state.
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startForeground(1, buildForegroundNotification());
        long minTimeMs;
        float minDistanceMeters;
        String provider;

        try {
            // Get minTimeMs and minDistanceMeters from intent
            minTimeMs = intent.getLongExtra("minTimeMs", 30000);
            minDistanceMeters = intent.getFloatExtra("minDistanceMeters", 10);
            provider = intent.getStringExtra("provider");
        } catch (Exception e) {
            AppLog.e("LocationForegroundService", "Failed to get minTimeMs, minDistanceMeters, or provider from intent. Using default values. Error: " + e.getMessage());
            minTimeMs = MIN_TIME_MS;
            minDistanceMeters = MIN_DISTANCE_METERS;
            provider = null;
        }
        if(provider == null) {
            provider = LocationManager.GPS_PROVIDER;
        }

        AppLog.d("LocationForegroundService", "Requesting location updates with minTimeMs=" + minTimeMs + ", minDistanceMeters=" + minDistanceMeters + ", provider=" + provider);
        try {
            locationManager.requestLocationUpdates(provider, minTimeMs, minDistanceMeters, new LocationReceiver(this.getApplicationContext()));
        } catch (SecurityException e) {
            AppLog.w("LocationForegroundService", "Failed to request location updates: " + e.getMessage());
        }

        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null; // Not used for this service
    }


    /**
     * Builds the persistent notification required for a foreground service.
     * This method sets up the notification channel and builds the notification that will
     * be shown as long as this service is in the foreground.
     *
     * @return The notification instance that describes this foreground service.
     */
    private Notification buildForegroundNotification() {
        NotificationChannel channel;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            channel = new NotificationChannel(CHANNEL_ID, "Location Service", NotificationManager.IMPORTANCE_LOW);
            channel.setDescription("No sound");
            channel.setSound(null, null); // No sound for this channel
            NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }

        // If this is a debug build we allow the notification to be swiped away
        boolean setOngoing = !BuildConfig.DEBUG;

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Elastic Agent Android")
                .setContentText("Location service is running")
                .setSmallIcon(R.drawable.icon)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOngoing(setOngoing);
        return builder.build();
    }
}
