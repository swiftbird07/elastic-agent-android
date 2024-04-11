package de.swiftbird.elasticandroid;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.preference.PreferenceManager;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.StyleSpan;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

/**
 * The main activity of the Elastic Agent Android application, serving as the entry point.
 * This activity displays the current enrollment status of the agent, provides options to
 * enroll/unenroll, sync data, view details, and access help, licenses, and legal information.
 *
 * <p>It implements the {@link StatusCallback} interface to handle callbacks from asynchronous
 * operations like enrollment and data synchronization.
 */
public class MainActivity extends AppCompatActivity implements StatusCallback {
    private static final String TAG = "MainActivity";
    private static final String CHANNEL_ID = "PermissionRequestChannel";
    private Button btnSyncNow;
    private Button btnEnrollUnenroll;
    private Button btnDetails;
    private FleetEnrollData enrollmentData;
    private TextView tAgentStatusEnrolled;
    private TextView tAgentStatusUnenrolled;

    /**
     * Initializes the activity, setting up UI components and event handlers. It also checks
     * if the app is being launched for the first time to show the legal disclaimer.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnSyncNow = findViewById(R.id.btnSyncNow);
        tAgentStatusEnrolled = findViewById(R.id.tAgentStatusEnrolled);
        tAgentStatusUnenrolled = findViewById(R.id.tAgentStatusUnenrolled);
        btnEnrollUnenroll = findViewById(R.id.btnEnrollUnenroll);
        btnDetails = findViewById(R.id.btnShowDetails);
        Button btnHelp = findViewById(R.id.btnHelp);
        Button btnLicense = findViewById(R.id.btnLicenses);
        Button btnLegal = findViewById(R.id.btnLegal);

        btnDetails.setOnClickListener(view -> startActivity(new Intent(MainActivity.this, DetailsActivity.class)));

        btnHelp.setOnClickListener(view -> startActivity(new Intent(MainActivity.this, HelpActivity.class)));

        btnLicense.setOnClickListener(view -> startActivity(new Intent(MainActivity.this, LicenseActivity.class)));

        btnLegal.setOnClickListener(view -> startActivity(new Intent(MainActivity.this, LegalActivity.class)));

        btnEnrollUnenroll.setOnClickListener(view -> {
            if (isEnrolled()) {
                showUnenrollmentDialog();
            } else {
                startActivity(new Intent(MainActivity.this, FleetEnrollActivity.class));
            }
        });

        // Load UI based on enrollment status from database
        try {
            AppDatabase db = AppDatabase.getDatabase(this.getApplicationContext(), "enrollment-data");
            db.enrollmentDataDAO().getEnrollmentInfo(1).observe(this, enrollmentData -> {

                // Set the click listener for the sync button after the enrollment data is loaded
                btnSyncNow.setOnClickListener(view -> onSyncNowClicked(db, enrollmentData));
            });
        } catch (Exception e) {
            AppLog.e(TAG, "Error loading enrollment data", e);
        }

        // Reset the 'firstTime' flag every time for debug builds
        if (BuildConfig.DEBUG) {
            // noinspection deprecation
            PreferenceManager.getDefaultSharedPreferences(this)
                    .edit()
                    .putBoolean("firstTime", false)
                    .apply();
        }

        // Check if it's the first app start
        // noinspection deprecation
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        if (!prefs.getBoolean("firstTime", false)) {
            // Show the legal disclaimer
            onLegalClicked();

            // After showing the disclaimer, set 'firstTime' to true
            prefs.edit().putBoolean("firstTime", true).apply();
        }

        // Create notification channel
        try {
            createNotificationChannel();
        } catch (Exception e) {
            AppLog.e(TAG, "Error creating notification channel", e);
        }

        onResume();

    }

    /**
     * onResume is overridden to refresh the UI based on the current enrollment status each
     * time the activity comes into the foreground.
     */
    @Override
    protected void onResume() {
        super.onResume();

        // Load UI based on enrollment status from database
        AppDatabase db = AppDatabase.getDatabase(this.getApplicationContext(), "enrollment-data");
        db.enrollmentDataDAO().getEnrollmentInfo(1).observe(this, enrollmentData -> {
            if(enrollmentData != null) {
                updateUIBasedOnEnrollment(enrollmentData);
            }
        });

        db.policyDataDAO().getPolicyData().observe(this, policyData -> {
            if(policyData != null) {
                btnSyncNow.setVisibility(View.VISIBLE);
            }
        });

        // We need to initialize the location receiver here as it needs the context later
        new LocationReceiver(this);
    }

    /**
     * onDestroy is overridden to remove all LiveData observers when the activity is destroyed.
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Remove all LiveData observers
        AppDatabase db = AppDatabase.getDatabase(this.getApplicationContext(), "enrollment-data");
        db.enrollmentDataDAO().getEnrollmentInfo(1).removeObservers(this);
        db.policyDataDAO().getPolicyData().removeObservers(this);
    }

    /**
     * Callback method for handling the sync button click event. This method initiates the checkin
     * process with the server and also resets the backoff intervals for the checkin and put workers.
     *
     * @param enrollmentData The current enrollment data.
     */
    protected void onSyncNowClicked(AppDatabase db, FleetEnrollData enrollmentData) {
        androidx.appcompat.app.AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setTitle("Checking in...")
                .setMessage("Starting checkin...")
                .setCancelable(false)
                .setPositiveButton("OK", /* listener = */ null)
                .show();
        FleetCheckinRepository checkinRepository = new FleetCheckinRepository(dialog, null);
        checkinRepository.checkinAgent(this, enrollmentData, AgentMetadata.getMetadataFromDeviceAndDB(enrollmentData.agentId, enrollmentData.hostname), this);

        // Also reset any backoff intervals and reschedule the workers so the user is able to fix any worker issues by clicking the sync button
        AppDatabase.databaseWriteExecutor.execute(() -> {
            db.policyDataDAO().resetBackoffCheckinInterval();
            db.policyDataDAO().resetBackoffPutInterval();
            PolicyData policyData = db.policyDataDAO().getPolicyDataSync();

            // Remove all registered workers
            WorkScheduler.cancelAllWork(getApplicationContext());
            // Now add the worker back
            int intervalCheckin = policyData.checkinInterval;
            WorkScheduler.scheduleFleetCheckinWorker(getApplicationContext(), intervalCheckin, TimeUnit.SECONDS, policyData.disableIfBatteryLow);
            int intervalPut = policyData.putInterval;
            WorkScheduler.scheduleElasticsearchWorker(getApplicationContext(), intervalPut, TimeUnit.SECONDS, policyData.disableIfBatteryLow);
        });
    }

    /**
     * Shows the legal disclaimer in a dialog box the first time the app is run.
     */
    protected void onLegalClicked() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Legal Disclaimer");
        String legalDisclaimerText = getString(R.string.legal_disclaimer_text);
        builder.setMessage(legalDisclaimerText);

        // Do nothing if the user accepts the legal disclaimer
        builder.setPositiveButton("ACCEPT", (dialog, id) -> {
        });
        // Close the app if the user declines the legal disclaimer
        builder.setNegativeButton("DECLINE", (dialog, id) -> finish());

        AlertDialog dialog = builder.create();
        dialog.show();
    }


    /**
     * Checks whether the agent is currently enrolled.
     *
     * @return {@code true} if the agent is enrolled, {@code false} otherwise.
     */
    private boolean isEnrolled() {
        try {
            return enrollmentData.isEnrolled;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Updates the UI components based on the enrollment data.
     *
     * @param enrollmentData The current enrollment data.
     */
    @SuppressLint("SetTextI18n") // Suppress warning for hardcoded strings, as App is English-only anyway
    private void updateUIBasedOnEnrollment(FleetEnrollData enrollmentData) {
        this.enrollmentData = enrollmentData;
        if (isEnrolled()) {
            // Enable the sync button
            btnSyncNow.setEnabled(true);
            btnSyncNow.setBackgroundColor(ContextCompat.getColor(this, R.color.elastic_agent_gray));
            btnSyncNow.setTextColor(ContextCompat.getColor(this, android.R.color.white));

            // Enable the details button
            btnDetails.setEnabled(true);
            btnDetails.setBackgroundColor(ContextCompat.getColor(this, R.color.elastic_agent_gray));
            btnDetails.setTextColor(ContextCompat.getColor(this, android.R.color.white));

            String formattedDate = formatDate(enrollmentData.enrolledAt);
            String baseText = "Agent is enrolled to " + enrollmentData.fleetUrl + " since " + formattedDate + ".";

            SpannableString spannableString = new SpannableString(baseText);

            // Make fleetUrl bold
            int fleetUrlStart = baseText.indexOf(enrollmentData.fleetUrl);
            int fleetUrlEnd = fleetUrlStart + enrollmentData.fleetUrl.length();
            spannableString.setSpan(new StyleSpan(Typeface.BOLD), fleetUrlStart, fleetUrlEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

            // Make the date bold
            int dateStart = baseText.indexOf(formattedDate);
            int dateEnd = dateStart + formattedDate.length();
            spannableString.setSpan(new StyleSpan(Typeface.BOLD), dateStart, dateEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

            tAgentStatusEnrolled.setText(spannableString);

            //tAgentStatusEnrolled.setText("Agent is enrolled to " + enrollmentData.fleetUrl + " since " + enrollmentData.enrolledAt + ".");
            tAgentStatusEnrolled.setVisibility(View.VISIBLE);
            tAgentStatusUnenrolled.setVisibility(View.INVISIBLE);

            // Update the button text for unenrollment
            btnEnrollUnenroll.setText("Unenroll Agent");
            btnEnrollUnenroll.setBackgroundColor(ContextCompat.getColor(this, android.R.color.holo_red_light));
            btnEnrollUnenroll.setTextColor(ContextCompat.getColor(this, android.R.color.white));
            btnEnrollUnenroll.setCompoundDrawablesWithIntrinsicBounds(android.R.drawable.ic_media_rew, 0, 0, 0);

        } else {
            tAgentStatusUnenrolled.setText("Agent is currently unenrolled.");
            tAgentStatusUnenrolled.setVisibility(View.VISIBLE);
            tAgentStatusEnrolled.setVisibility(View.INVISIBLE);

            // Update the button text for enrollment
            btnEnrollUnenroll.setText("Enroll Agent");

            // Set color back to elastic_agent_green
            btnEnrollUnenroll.setBackgroundColor(ContextCompat.getColor(this, R.color.elastic_agent_green));
            btnEnrollUnenroll.setTextColor(ContextCompat.getColor(this, android.R.color.white));
            btnEnrollUnenroll.setCompoundDrawablesWithIntrinsicBounds(android.R.drawable.ic_media_ff, 0, 0, 0);

            // Disable the sync button
            btnSyncNow.setEnabled(false);
            btnSyncNow.setBackgroundColor(ContextCompat.getColor(this, android.R.color.darker_gray));
            btnSyncNow.setTextColor(ContextCompat.getColor(this, android.R.color.white));
            // Disable the details button
            btnDetails.setEnabled(false);
            btnDetails.setBackgroundColor(ContextCompat.getColor(this, android.R.color.darker_gray));
            btnDetails.setTextColor(ContextCompat.getColor(this, android.R.color.white));

        }
    }



    /**
     * Shows a dialog confirming the agent's unenrollment.
     */
    private void showUnenrollmentDialog() {
        new AlertDialog.Builder(this)
                .setMessage("Are you sure you want to unenroll?")
                .setPositiveButton("Yes", (dialog, which) -> unenrollAgent())
                .setNegativeButton("No", null)
                .show();
    }

    /**
     * Performs the unenrollment of the agent, including cleanup of data and stopping any
     * ongoing operations.
     */
    private void unenrollAgent() {
        // TODO: Implement sending last data log to ES
        // Remove all registered workers
        WorkScheduler.cancelAllWork(getApplicationContext());

        // Delete the enrollment data from the database
        AppDatabase db = AppDatabase.getDatabase(this.getApplicationContext(), "enrollment-data");
        AppDatabase.databaseWriteExecutor.execute(() -> db.enrollmentDataDAO().delete());
        AppDatabase.databaseWriteExecutor.execute(() -> db.policyDataDAO().delete());
        AppDatabase.databaseWriteExecutor.execute(() -> db.selfLogCompBuffer().deleteAllDocuments());
        AppDatabase.databaseWriteExecutor.execute(() -> db.statisticsDataDAO().delete());

        // Refresh the UI
        updateUIBasedOnEnrollment(new FleetEnrollData());
    }

    /**
     * Callback method for handling the result of asynchronous enrollment and checkin operations.
     *
     * @param success {@code true} if the operation was successful, {@code false} otherwise.
     */
    @Override
    public void onCallback(boolean success) {
        if (success) {
            runOnUiThread(() -> { // Run on the UI thread is necessary because the callback could be called from a background thread
                Toast.makeText(getApplicationContext(), "Checkin successful", Toast.LENGTH_SHORT).show();
                onResume();
            });

        } else {
            runOnUiThread(() -> Toast.makeText(getApplicationContext(), "Checkin failed", Toast.LENGTH_SHORT).show());
        }
    }

    /**
     * Formats a date string from one pattern to another (used in the top text view to show the enrollment date).
     *
     * @param dateUnformatted The unformatted date string.
     * @return The formatted date string.
     */
    private String formatDate(String dateUnformatted){
        try {
            String originalPattern = "yyyy-MM-dd";
            String desiredPattern = "MMMM d, yyyy";
            SimpleDateFormat originalFormat = new SimpleDateFormat(originalPattern, Locale.getDefault());
            SimpleDateFormat desiredFormat = new SimpleDateFormat(desiredPattern, Locale.getDefault());

            Date date = originalFormat.parse(dateUnformatted);
            if(date == null){ // If the date could not be parsed, return the original string
                return dateUnformatted;
            }

            return desiredFormat.format(date);
        } catch (ParseException e) {
            AppLog.w(TAG, "Could not parse date " + dateUnformatted);
            return dateUnformatted;
        }

    }

    /**
     * Creates a notification channel for the app. This is required for API level 26 and above.
     */
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Elastic Agent Android";
            String description = "Elastic Agent Android Notification Channel";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }
}
