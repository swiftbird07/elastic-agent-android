package de.swiftbird.elasticandroid;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.IBinder;
import android.os.Bundle;

import androidx.core.app.NotificationCompat;

public class LocationForegroundService extends Service implements LocationListener {

    private LocationManager locationManager;

    @Override
    public void onCreate() {
        super.onCreate();
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
    }

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
            AppLog.e("LocationForegroundService", "Failed to get minTimeMs and minDistanceMeters from intent: " + e.getMessage());
            minTimeMs = 300000;
            minDistanceMeters = 10;
            provider = null;
        }
        if(provider == null) {
            provider = LocationManager.GPS_PROVIDER;
        }

        // Assuming permissions have been granted
        AppLog.d("LocationForegroundService", "Requesting location updates with minTimeMs=" + minTimeMs + ", minDistanceMeters=" + minDistanceMeters + ", provider=" + provider);
        try {
            locationManager.requestLocationUpdates(provider, minTimeMs, minDistanceMeters, new LocationReceiver(this.getApplicationContext()));
        } catch (SecurityException e) {
            AppLog.w("LocationForegroundService", "Failed to request location updates: " + e.getMessage());
        }

        return START_STICKY;
    }

    @Override
    public void onLocationChanged(Location location) {
        // Handle each location update
    }

    // Implement other LocationListener methods

    @Override
    public IBinder onBind(Intent intent) {
        return null; // Not used for this service
    }

    // Method to build the foreground service notification
    private Notification buildForegroundNotification() {
        NotificationChannel channel;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            channel = new NotificationChannel("location_service", "Location Service", NotificationManager.IMPORTANCE_LOW);
            channel.setDescription("No sound");
            channel.setSound(null, null); // No sound for this channel
            NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }

        boolean setOngoing = true;
        // If this is a debug build we allow the notification to be swiped away
        if (BuildConfig.DEBUG) {
            setOngoing = false;
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "location_service")
                .setContentTitle("Elastic Agent Android")
                .setContentText("Location service is running")
                .setSmallIcon(R.drawable.icon)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOngoing(setOngoing);
        return builder.build();
    }

}
