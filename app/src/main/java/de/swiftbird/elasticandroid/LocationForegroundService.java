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
        startForeground(1, buildForegroundNotification()); // Build your notification here

        // Assuming permissions have been granted
        try {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
        } catch (SecurityException e) {
            // Handle exception
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
            channel = new NotificationChannel("location_service", "Location Service", NotificationManager.IMPORTANCE_DEFAULT);
            NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "location_service")
                .setContentTitle("Location Tracking Active")
                .setContentText("Your location is being tracked by the app.")
                .setSmallIcon(R.drawable.icon) // Ensure you have a drawable resource for the icon
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        return builder.build();
    }

}
