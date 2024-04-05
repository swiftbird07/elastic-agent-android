package de.swiftbird.elasticandroid;

import android.content.Context;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;

import java.util.Arrays;
import java.util.List;

public class LocationComp implements Component {

    private static final String TAG = "LocationComp";
    private LocationCompBuffer buffer;
    private AppStatisticsDataDAO statistic;
    private static LocationComp locationComp;
    private LocationManager locationManager;
    private LocationListener locationListener;
    private HandlerThread locationHandlerThread;
    private Handler locationHandler;

    public static LocationComp getInstance() {
        // Singleton pattern
        if (locationComp == null) {
            locationComp = new LocationComp();
        }
        return locationComp;
    }


    @Override
    public boolean setup(Context context, FleetEnrollData enrollmentData, PolicyData policyData, String subComponent) {
        AppLog.d(TAG, "Setting up location component");
        AppDatabase db = AppDatabase.getDatabase(context, "");
        this.buffer = db.locationCompBuffer();
        this.statistic = db.statisticsDataDAO();

        locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        locationHandlerThread = new HandlerThread("LocationHandlerThread");
        locationHandlerThread.start();
        locationHandler = new Handler(locationHandlerThread.getLooper());
        locationListener = new LocationReceiver(context);

        // Parse subComponent as url parameters for minTimeMs and minDistanceMeters.
        boolean paramsSet = true;
        String[] paramPairs = null;
        String[] params = subComponent.split("\\?");

        if (params.length != 2) {
            paramsSet = false; // in this case, no parameters are set and we later request location updates with default values
        } else {
            paramPairs = params[1].split("&");
        }

        if (paramsSet && paramPairs.length != 2) {
            AppLog.w(TAG, "Can't setup. Parameters set but need exactly 2 parameters in path but got " + paramPairs.length);
            return false;
        }

        int minTimeMs = 0;
        int minDistanceMeters = 0;

        if (paramsSet) {
            for (String paramPair : paramPairs) {
                String[] pair = paramPair.split("=");
                if (pair.length != 2) {
                    AppLog.w(TAG, "Can't setup. Parameters set and invalid parameter in path: " + subComponent);
                    return false;
                }
                if ("minTimeMs".equals(pair[0])) {
                    minTimeMs = Integer.parseInt(pair[1]);
                } else if ("minDistanceMeters".equals(pair[0])) {
                    minDistanceMeters = Integer.parseInt(pair[1]);
                } else {
                    AppLog.w(TAG, "Can't setup. Unknown parameter: " + pair[0] + " in path: " + subComponent);
                    return false;
                }
            }
        } else {
            AppLog.w(TAG, "No parameters set. Using default values for minTimeMs and minDistanceMeters");
            minTimeMs = 30000; // 30 seconds
            minDistanceMeters = 10; // 10 meters
        }

        subComponent = params[0]; // get the subComponent without parameters
        if (subComponent == null || subComponent.isEmpty()) {
            AppLog.w(TAG, "Can't setup. No sub-component provided");
            return false;
        }

        int finalMinTimeMs = minTimeMs;
        int finalMinDistanceMeters = minDistanceMeters;

        if ("coarse".equals(subComponent)) {
            // Setup the component for coarse location
            requestLocationUpdates(LocationManager.NETWORK_PROVIDER, finalMinTimeMs, finalMinDistanceMeters);
        } else if ("fine".equals(subComponent)) {
            // Setup the component for fine location
            requestLocationUpdates(LocationManager.GPS_PROVIDER, finalMinTimeMs, finalMinDistanceMeters);
        } else {
            AppLog.w(TAG, "Can't setup. Unknown sub-component: " + subComponent);
            return false;
        }
        return true;
    }

    public void setup_light(Context context) {
        AppDatabase db = AppDatabase.getDatabase(context, "");
        this.buffer = db.locationCompBuffer();
        this.statistic = db.statisticsDataDAO();
    }

    private void requestLocationUpdates(String provider, int minTimeMs, int minDistanceMeters) {
        try {
            locationManager.requestLocationUpdates(provider, minTimeMs, minDistanceMeters, locationListener, locationHandlerThread.getLooper());
            AppLog.d(TAG, "Location updates requested for provider: " + provider + " with minTimeMs: " + minTimeMs + " and minDistanceMeters: " + minDistanceMeters);
        } catch (SecurityException e) {
            AppLog.e(TAG, "Failed to request location updates because of missing permissions: " + e.getMessage());
        }
    }

    @Override
    public void collectEvents(FleetEnrollData enrollmentData, PolicyData policyData) {
        // No-op for this component (location updates are collected in real-time)
    }

    @Override
    public void addDocumentToBuffer(ElasticDocument document) {
        if (document instanceof LocationCompDocument && buffer != null) {
            buffer.insertDocument((LocationCompDocument) document);
            statistic.increaseCombinedBufferSize(1);
        } else {
            Log.w(TAG, "Invalid document type or buffer not initialized");
        }
    }

    @Override
    public <T extends ElasticDocument> List<T> getDocumentsFromBuffer(int maxDocuments) {
        int toIndex = Math.min(maxDocuments, buffer.getDocumentCount());
        List<LocationCompDocument> logBuffer = buffer.getOldestDocuments(toIndex);
        buffer.deleteOldestDocuments(toIndex);

        @SuppressWarnings("unchecked") // Safe cast
        List<T> result = (List<T>) logBuffer;
        return result;
    }

    @Override
    public int getDocumentsInBufferCount() {
        return buffer.getDocumentCount();
    }

    @Override
    public List<String> getRequiredPermissions() {
        // Precise and/or coarse location as well as background location are required
        return Arrays.asList(
                "android.permission.ACCESS_FINE_LOCATION",
                "android.permission.ACCESS_COARSE_LOCATION",
                "android.permission.ACCESS_BACKGROUND_LOCATION"
        );
    }

    @Override
    public String getPathName() {
        return "location";
    }

    @Override
    public void disable(Context context, FleetEnrollData enrollmentData, PolicyData policyData) {
        AppLog.d(TAG, "Disabling location updates");
        locationManager.removeUpdates(locationListener);
    }

}
