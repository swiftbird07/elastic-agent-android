package de.swiftbird.elasticandroid;

import androidx.annotation.NonNull;
import android.content.Context;

/**
 * LocationReceiver is a custom {@link android.location.LocationListener} implementation designed to
 * handle location updates. When a new location is received, it processes the location data and
 * manages the creation and buffering of {@link LocationCompDocument} instances, which are then stored
 * for later use or transmission.
 *
 * This class initiates a new Thread to handle each location update to ensure that the processing
 * does not interfere with the UI or other main thread operations, promoting smoother performance
 * and better user experience.
 */
public class LocationReceiver implements android.location.LocationListener{
    private final Context context;

    /**
     * Constructs a LocationReceiver with the specified application context.
     * Using application context helps avoid potential memory leaks that could occur
     * if an activity context was used.
     *
     * @param context The application context used for database and logging operations.
     */
    public LocationReceiver(Context context) {
        // Use application context to avoid potential memory leaks
        this.context = context.getApplicationContext();
    }

    /**
     * Called when the location has changed. This method is triggered when a new location update.
     * It will execute the {@link #handleLocationUpdate(android.location.Location)} method on a new thread.
     *
     * @param location The new location, as a {@link android.location.Location} object.
     */
    @Override
    public void onLocationChanged(@NonNull android.location.Location location) {
        // New thread to handle the location update
        new Thread(() -> handleLocationUpdate(location)).start();
    }

    /**
     * This method retrieves or creates necessary components and database access objects,
     * creates a new {@link LocationCompDocument} with the location data, and then adds this
     * document to the buffer for later processing or transmission.
     *
     * @param location The location data to be processed.
     */
    private void handleLocationUpdate(android.location.Location location) {
        // Handle each location update
        AppLog.d("LocationReceiver", "Location changed: " + location);
        LocationComp locationComp = LocationComp.getInstance();
        locationComp.setup_light(context);
        AppDatabase db = AppDatabase.getDatabase(context, "");
        PolicyData policyData = db.policyDataDAO().getPolicyDataSync();
        FleetEnrollData enrollmentData = db.enrollmentDataDAO().getEnrollmentInfoSync(1);
        try {
            locationComp.addDocumentToBuffer(new LocationCompDocument(location, enrollmentData, policyData, context));
        } catch (Exception e) {
            AppLog.e("LocationReceiver", "Error adding location document to buffer", e);
        }
    }

    @Override
    public void onStatusChanged(String provider, int status, android.os.Bundle extras) {}

    @Override
    public void onProviderEnabled(@NonNull String provider) {}

    @Override
    public void onProviderDisabled(@NonNull String provider) {}
};

