package de.swiftbird.elasticandroid;

import static androidx.core.content.ContextCompat.startForegroundService;

import android.content.Context;
import android.content.Intent;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;

import java.util.Arrays;
import java.util.List;

/**
 * Optional component that implements the Component interface, specializing in handling location data.
 * It encapsulates functionality for setting up location tracking, managing location updates, and buffering
 * received location data for later processing or transmission. This component supports different location
 * accuracy modes and leverages Android's LocationManager for obtaining location updates.
 * <p>
 * For missing documentation, refer to the Component interface.
 */
public class LocationComp implements Component {

    private static final String TAG = "LocationComp";
    private LocationCompBuffer buffer;
    private AppStatisticsDataDAO statistic;
    private static LocationComp locationComp;
    private LocationManager locationManager;
    private LocationListener locationListener;
    private HandlerThread locationHandlerThread;

    // Default values for location updates
    private static final long DEFAULT_MIN_TIME_MS = 1000 * 60 * 15; // 15 minutes
    private static final float DEFAULT_MIN_DISTANCE_METERS = 1000; // 1 km

    /**
     * Provides access to the singleton instance of LocationComp, creating it if necessary.
     * @return The singleton instance of LocationComp.
     */
    public static synchronized LocationComp getInstance() {
        // Singleton pattern
        if (locationComp == null) {
            locationComp = new LocationComp();
        }
        return locationComp;
    }


    /**
     * Configures the location component based on specified settings. It initializes required resources,
     * such as database access objects and the location listener, and starts location updates with specified
     * parameters if necessary.
     *
     * @param context The application context.
     * @param enrollmentData Data related to the enrollment of the device.
     * @param policyData The current policy data affecting location tracking.
     * @param subComponent A string representing specific settings or configurations for location tracking.
     * @return A boolean indicating whether setup was successful.
     */
    @Override
    public boolean setup(Context context, FleetEnrollData enrollmentData, PolicyData policyData, String subComponent) {
        AppLog.d(TAG, "Setting up location component");
        AppDatabase db = AppDatabase.getDatabase(context, "");
        this.buffer = db.locationCompBuffer();
        this.statistic = db.statisticsDataDAO();

        locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        locationHandlerThread = new HandlerThread("LocationHandlerThread");
        locationHandlerThread.start();
        Handler locationHandler = new Handler(locationHandlerThread.getLooper());
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

        long minTimeMs = 0;
        float minDistanceMeters = 0;
        try {
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
                minTimeMs = DEFAULT_MIN_TIME_MS;
                minDistanceMeters = DEFAULT_MIN_DISTANCE_METERS;
            }
        } catch (NumberFormatException e) {
            AppLog.w(TAG, "Can't setup. Invalid number format in path: " + subComponent);
            return false;
        }

        subComponent = params[0]; // get the subComponent without parameters
        if (subComponent == null || subComponent.isEmpty()) {
            AppLog.w(TAG, "Can't setup. No sub-component provided");
            return false;
        }

        long finalMinTimeMs = minTimeMs;
        float finalMinDistanceMeters = minDistanceMeters;
        String provider;

        if ("coarse".equals(subComponent)) {
            // Setup the component for coarse location
            AppLog.d(TAG, "Setting up location component for coarse location");
            provider = LocationManager.NETWORK_PROVIDER;
        } else if ("fine".equals(subComponent)) {
            // Setup the component for fine location
            AppLog.d(TAG, "Setting up location component for fine location");
            provider = LocationManager.GPS_PROVIDER;
        } else {
            AppLog.w(TAG, "Can't setup. Unknown sub-component: " + subComponent);
            return false;
        }

        // For background location updates enable sticky notification
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Intent serviceIntent = new Intent(context, LocationForegroundService.class);
            serviceIntent.putExtra("minTimeMs", finalMinTimeMs);
            serviceIntent.putExtra("minDistanceMeters", finalMinDistanceMeters);
            serviceIntent.putExtra("provider", provider);
            context.startForegroundService(serviceIntent);
        } else {
            AppLog.w(TAG, "Can't setup. Background location updates are not supported on this device");
            return false;
        }
        return true;
    }

    /**
     * Performs a lightweight setup, initializing only essential resources without starting location updates.
     * This can be used in scenarios where full setup is not required immediately upon device startup.
     *
     * @param context The application context.
     */
    public void setup_light(Context context) {
        AppDatabase db = AppDatabase.getDatabase(context, "");
        this.buffer = db.locationCompBuffer();
        this.statistic = db.statisticsDataDAO();
    }

    /**
     * Initiates the collection of events. This is a no-op for this component as location updates are
     * collected in real-time via the locationListener.
     *
     * @param enrollmentData Data related to the enrollment of the device.
     * @param policyData The current policy data.
     */
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
        locationHandlerThread.quitSafely();
    }

}
