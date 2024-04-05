package de.swiftbird.elasticandroid;

import androidx.annotation.NonNull;
import android.content.Context;


public class LocationReceiver implements android.location.LocationListener{
    private final Context context;

    // Constructor to pass in context
    public LocationReceiver(Context context) {
        // Use application context to avoid potential memory leaks
        this.context = context.getApplicationContext();
    }

    @Override
    public void onLocationChanged(@NonNull android.location.Location location) {
        // Handle location update
        AppLog.d("LocationReceiver", "Location changed: " + location);
        LocationComp locationComp = LocationComp.getInstance();
        locationComp.setup_light(context);
        AppDatabase db = AppDatabase.getDatabase(context, "");
        PolicyData policyData = db.policyDataDAO().getPolicyDataSync();
        FleetEnrollData enrollmentData = db.enrollmentDataDAO().getEnrollmentInfoSync(1);
        locationComp.addDocumentToBuffer(new LocationCompDocument(location, enrollmentData, policyData));
    }

    @Override
    public void onStatusChanged(String provider, int status, android.os.Bundle extras) {}

    @Override
    public void onProviderEnabled(@NonNull String provider) {}

    @Override
    public void onProviderDisabled(@NonNull String provider) {}
};

