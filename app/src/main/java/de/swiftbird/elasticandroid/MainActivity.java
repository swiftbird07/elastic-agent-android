package de.swiftbird.elasticandroid;

import android.app.AlertDialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import androidx.appcompat.app.AppCompatActivity;

import android.preference.PreferenceManager;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.StyleSpan;
import android.view.View;

import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity implements StatusCallback {

    private Button btnSyncNow;
    private Button btnEnrollUnenroll;

    private Button btnDetails;

    private Button btnHelp;
    private Button btnLicense;
    private Button btnLegal;

    private LinearLayout llEnrollmentDetails;

    private FleetEnrollData enrollmentData;

    private TextView tAgentStatusEnrolled;
    private TextView tAgentStatusUnenrolled;

    private RelativeLayout.LayoutParams layoutParams;

    private String TAG = "MainActivity";
    private static final String CHANNEL_ID = "PermissionRequestChannel";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        btnSyncNow = findViewById(R.id.btnSyncNow);
        tAgentStatusEnrolled = findViewById(R.id.tAgentStatusEnrolled);
        tAgentStatusUnenrolled = findViewById(R.id.tAgentStatusUnenrolled);
        btnEnrollUnenroll = findViewById(R.id.btnEnrollUnenroll);
        btnDetails = findViewById(R.id.btnShowDetails);

        btnDetails.setOnClickListener(view -> {
            startActivity(new Intent(MainActivity.this, DetailsActivity.class));
        });

        btnHelp = findViewById(R.id.btnHelp);
        btnLicense = findViewById(R.id.btnLicenses);
        btnLegal = findViewById(R.id.btnLegal);

        btnHelp.setOnClickListener(view -> {
            startActivity(new Intent(MainActivity.this, HelpActivity.class));
        });

        btnLicense.setOnClickListener(view -> {
            startActivity(new Intent(MainActivity.this, LicenseActivity.class));
        });

        btnLegal.setOnClickListener(view -> {
            startActivity(new Intent(MainActivity.this, LegalActivity.class));
        });

        btnEnrollUnenroll.setOnClickListener(view -> {
            if (isEnrolled()) {
                showUnenrollmentDialog();
            } else {
                startActivity(new Intent(MainActivity.this, FleetEnrollActivity.class));
            }
        });



        // Load UI based on enrollment status from database
        AppDatabase db = AppDatabase.getDatabase(this.getApplicationContext(), "enrollment-data");
        db.enrollmentDataDAO().getEnrollmentInfo(1).observe(this, enrollmentData -> {
            updateUIBasedOnEnrollment(enrollmentData);

            btnSyncNow.setOnClickListener(view -> {
                FleetCheckinRepository checkinRepository = FleetCheckinRepository.getInstance(this);
                androidx.appcompat.app.AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                        .setTitle("Checking in...")
                        .setMessage("Starting checkin...")
                        .setCancelable(false)
                        .setPositiveButton("OK", /* listener = */ null)
                        .show();
                checkinRepository.checkinAgent(enrollmentData, AgentMetadata.getMetadataFromDeviceAndDB(enrollmentData.agentId, enrollmentData.hostname), this, dialog, null, this);

                // Also reset any backoff intervals
                AppDatabase.databaseWriteExecutor.execute(() -> {
                    db.policyDataDAO().resetBackoffCheckinInterval();
                    db.policyDataDAO().resetBackoffPutInterval();
                    PolicyData policyData =  db.policyDataDAO().getPolicyDataSync();

                    // Remove all registered workers
                    WorkScheduler.cancelAllWork(getApplicationContext());
                    // Now add the worker back
                    int intervalCheckin = policyData.checkinInterval;
                    WorkScheduler.scheduleFleetCheckinWorker(getApplicationContext(), intervalCheckin, TimeUnit.SECONDS);
                    int intervalPut = policyData.putInterval;
                    WorkScheduler.scheduleElasticsearchWorker(getApplicationContext(), intervalPut, TimeUnit.SECONDS);
                });
            });
        });

        // Reset the 'firstTime' flag every time for debug builds
        if (BuildConfig.DEBUG) {
            PreferenceManager.getDefaultSharedPreferences(this)
                    .edit()
                    .putBoolean("firstTime", false)
                    .apply();
        }


        // Check if it's the first app start
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        if (!prefs.getBoolean("firstTime", false)) {
            // Show the legal disclaimer
            showLegalDisclaimer();

            // After showing the disclaimer, set 'firstTime' to true
            prefs.edit().putBoolean("firstTime", true).apply();
        }

        onResume();

    }

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

        db.policyDataDAO().getPoliyData().observe(this, policyData -> {
            if(policyData != null) {
                btnSyncNow.setVisibility(View.VISIBLE);
            }
        });

        // We nned to initialize the location receiver here as it needs the context later
        LocationReceiver locationReceiver = new LocationReceiver(this);
    }

    private boolean isEnrolled() {
        try {
            return enrollmentData.isEnrolled;
        } catch (Exception e) {
            return false;
        }
    }

    private void updateUIBasedOnEnrollment(FleetEnrollData enrollmentData) {
        this.enrollmentData = enrollmentData;
        if (isEnrolled()) {
            // Enable the sync button
            btnSyncNow.setEnabled(true);
            btnSyncNow.setBackgroundColor(getResources().getColor(R.color.elastic_agent_gray));
            btnSyncNow.setTextColor(getResources().getColor(android.R.color.white));
            // Enable the details button
            btnDetails.setEnabled(true);
            btnDetails.setBackgroundColor(getResources().getColor(R.color.elastic_agent_gray));
            btnDetails.setTextColor(getResources().getColor(android.R.color.white));

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
            btnEnrollUnenroll.setBackgroundColor(getResources().getColor(android.R.color.holo_red_light));
            btnEnrollUnenroll.setTextColor(getResources().getColor(android.R.color.white));
            btnEnrollUnenroll.setCompoundDrawablesWithIntrinsicBounds(android.R.drawable.ic_media_rew, 0, 0, 0);

        } else {
            tAgentStatusUnenrolled.setText("Agent is currently unenrolled.");
            tAgentStatusUnenrolled.setVisibility(View.VISIBLE);
            tAgentStatusEnrolled.setVisibility(View.INVISIBLE);

            // Update the button text for enrollment
            btnEnrollUnenroll.setText("Enroll Agent");
            // Set color back to elastic_agent_green
            btnEnrollUnenroll.setBackgroundColor(getResources().getColor(R.color.elastic_agent_green));
            btnEnrollUnenroll.setTextColor(getResources().getColor(android.R.color.white));
            btnEnrollUnenroll.setCompoundDrawablesWithIntrinsicBounds(android.R.drawable.ic_media_ff, 0, 0, 0);

            // Disable the sync button
            btnSyncNow.setEnabled(false);
            btnSyncNow.setBackgroundColor(getResources().getColor(android.R.color.darker_gray));
            btnSyncNow.setTextColor(getResources().getColor(android.R.color.white));
            // Disable the details button
            btnDetails.setEnabled(false);
            btnDetails.setBackgroundColor(getResources().getColor(android.R.color.darker_gray));
            btnDetails.setTextColor(getResources().getColor(android.R.color.white));

        }
    }




    private void showUnenrollmentDialog() {
        new AlertDialog.Builder(this)
                .setMessage("Are you sure you want to unenroll?")
                .setPositiveButton("Yes", (dialog, which) -> unenrollAgent())
                .setNegativeButton("No", null)
                .show();
    }

    private void unenrollAgent() {
        // TODO: Implement sending last data log to ES
        // Remove all registered workers
        WorkScheduler.cancelAllWork(getApplicationContext());

        // Delete the enrollment data from the database
        AppDatabase db = AppDatabase.getDatabase(this.getApplicationContext(), "enrollment-data");
        AppDatabase.databaseWriteExecutor.execute(() -> {
            db.enrollmentDataDAO().delete();
        });

        AppDatabase.databaseWriteExecutor.execute(() -> {
            db.policyDataDAO().delete();
        });

        AppDatabase.databaseWriteExecutor.execute(() -> {
            db.selfLogCompBuffer().deleteAllDocuments();
        });

        AppDatabase.databaseWriteExecutor.execute(() -> {
            db.statisticsDataDAO().delete();
        });

        // Refresh the UI
        updateUIBasedOnEnrollment(new FleetEnrollData());
    }

    @Override
    public void onCallback(boolean success) {
        if (success) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getApplicationContext(), "Checkin successful", Toast.LENGTH_SHORT).show();
                    onResume();
                }
            });

        } else {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getApplicationContext(), "Checkin failed", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private String formatDate(String dateUnformatted){
        try {
            String originalPattern = "yyyy-MM-dd"; // Assume original format
            String desiredPattern = "MMMM d, yyyy";
            SimpleDateFormat originalFormat = new SimpleDateFormat(originalPattern, Locale.getDefault());
            SimpleDateFormat desiredFormat = new SimpleDateFormat(desiredPattern, Locale.getDefault());

            Date date = originalFormat.parse(dateUnformatted);

            return desiredFormat.format(date);
        } catch (ParseException e) {
            AppLog.w(TAG, "Could not parse date " + dateUnformatted);
            return dateUnformatted;
        }

    }

    private void showLegalDisclaimer() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Legal Disclaimer");
        String legalDisclaimerText =
                "This software is provided 'as is', without warranty of any kind, express or " +
                        "implied, including but not limited to the warranties of merchantability, " +
                        "fitness for a particular purpose and noninfringement. In no event shall the " +
                        "authors or copyright holders be liable for any claim, damages or other " +
                        "liability, whether in an action of contract, tort or otherwise, arising from, " +
                        "out of or in connection with the software or the use or other dealings in the " +
                        "software.\n\n" +
                        "It is strictly prohibited to use this software for illegal activities, including " +
                        "but not limited to spying on individuals without their explicit consent. All users " +
                        "and individuals who have the software installed on their device must be duly informed " +
                        "about its functionalities and purposes.\n\n" +
                        "The developer of this software disclaims all liability for misuse or illegal use " +
                        "of the software. Users are responsible for ensuring their use of the software complies " +
                        "with all applicable laws and regulations.";
        builder.setMessage(legalDisclaimerText);

        builder.setPositiveButton("ACCEPT", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
            }
        });
        builder.setNegativeButton("DECLINE", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                finish();
            }
        });

        AlertDialog dialog = builder.create();
        dialog.show();
    }

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
