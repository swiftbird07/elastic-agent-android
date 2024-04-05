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

public class PermissionRequestActivity extends Activity {
    private static final int PERMISSION_REQUEST_CODE = 1; // For foreground location
    private static final int PERMISSION_BACKGROUND_REQUEST_CODE = 2; // For background location

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

    private void requestBackgroundLocationPermission() {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_BACKGROUND_LOCATION}, PERMISSION_BACKGROUND_REQUEST_CODE);
    }

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

    public static boolean hasPermissions(Context context, String[] permissions) {
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }
}
