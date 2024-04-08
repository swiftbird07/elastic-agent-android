package de.swiftbird.elasticandroid;

import android.Manifest;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;

/**
 * An {@link Activity} for requesting necessary permissions from the user.
 * This activity is designed to handle foreground and background location permissions
 * but can be adapted for other permissions as well.
 *
 * <p>It checks if the necessary permissions have already been granted.
 * If not, it requests the user to grant them. This is essential for ensuring
 * the app's location-based features function correctly, especially for newer
 * Android versions where background location access has additional restrictions.</p>
 *
 * <p>The activity also provides utility methods for showing a permission notification
 * and checking if all required permissions have been granted.</p>
 */
public class PermissionRequestActivity extends Activity {
    private static final int PERMISSION_REQUEST_CODE = 1; // For foreground location
    private static final int PERMISSION_BACKGROUND_REQUEST_CODE = 2; // For background location

    /**
     * Creates the activity and checks if the necessary permissions have been granted.
     * If not, it requests the user to grant them.
     *
     * @param savedInstanceState The saved instance state.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        String[] permissionsNeeded = getIntent().getStringArrayExtra("permissions");
        if (permissionsNeeded == null) {
            AppLog.e("PermissionRequestActivity", "No permissions provided to request");
            finish();
            return;
        }
        checkAndRequestPermissions(permissionsNeeded);
    }

    /**
     * Checks the provided permissions and requests any that haven't been granted yet.
     * This method handles foreground and background location permissions.
     *
     * @param permissionsNeeded An array of permissions the app needs to function correctly.
     */
    private void checkAndRequestPermissions(String[] permissionsNeeded) {
        List<String> permissionsToRequest = new ArrayList<>();
        boolean backgroundLocationNeeded = false;

        for (String permission : permissionsNeeded) {
            if (permission.equals(Manifest.permission.ACCESS_BACKGROUND_LOCATION) && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                backgroundLocationNeeded = true;
                continue;
            }
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(permission);
            } else {
                AppLog.d("PermissionRequestActivity", "Permission already granted: " + permission);
            }
        }

        if (!permissionsToRequest.isEmpty()) {
            AppLog.i("PermissionRequestActivity", "Requesting permissions: " + permissionsToRequest);
            ActivityCompat.requestPermissions(this, permissionsToRequest.toArray(new String[0]), PERMISSION_REQUEST_CODE);
        } else if (backgroundLocationNeeded) {
            AppLog.i("PermissionRequestActivity", "Requesting background location permission");
            Toast.makeText(this, "Please enable background location permission", Toast.LENGTH_LONG).show();
            requestBackgroundLocationPermission();
        } else {
            finish(); // Close activity if no permissions need to be requested
        }
    }

    /**
     * Specifically requests the background location permission from the user.
     * This is separate due to its special handling post-Android Q (API level 29).
     */
    private void requestBackgroundLocationPermission() {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_BACKGROUND_LOCATION}, PERMISSION_BACKGROUND_REQUEST_CODE);
    }

    /**
     * Handles the result of the permission request.
     *
     * @param requestCode The request code for the permission request.
     * @param permissions The permissions requested.
     * @param grantResults The results of the permission request.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            boolean allPermissionsGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allPermissionsGranted = false;
                    break;
                }
            }
            if (allPermissionsGranted && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                requestBackgroundLocationPermission();
            } else {
                finish(); // Finish regardless of the outcome for this example
            }
        } else if (requestCode == PERMISSION_BACKGROUND_REQUEST_CODE) {
            finish(); // Finish after attempting to request background location permission
        }
    }

    /**
     * Displays a notification prompting the user to grant the necessary permissions.
     * Tapping the notification opens this activity to handle the permission request.
     *
     * @param context The application context used for showing the notification.
     */
    public static void showPermissionNotification(Context context) {
        Intent intent = new Intent(context, PermissionRequestActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, "PermissionRequestChannel")
                .setSmallIcon(R.drawable.icon)
                .setContentTitle("Permission(s) Needed")
                .setContentText("Tap to enable permissions required for agent operation.")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        try {
            notificationManager.notify(1, builder.build());
        } catch (SecurityException e) {
            // TODO: Handle exception of user not giving notification permission
            AppLog.w("PermissionRequestActivity", "Failed to show notification because of missing permissions: " + e.getMessage());
        }

    }

    /**
     * Checks if all specified permissions have been granted.
     *
     * @param context The context to check permissions against.
     * @param permissions An array of permissions to check.
     * @return {@code true} if all permissions have been granted, {@code false} otherwise.
     */
    public static boolean hasPermissions(Context context, String[] permissions) {
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }
}
