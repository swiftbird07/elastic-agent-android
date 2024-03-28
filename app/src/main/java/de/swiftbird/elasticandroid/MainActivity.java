package de.swiftbird.elasticandroid;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import androidx.appcompat.app.AppCompatActivity;

import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.util.Log;
import android.view.View;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.core.content.ContextCompat;

import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements StatusCallback {

    private Button btnSyncNow;
    private Button btnEnrollUnenroll;

    private Button btnDetails;

    private LinearLayout llEnrollmentDetails;

    private EnrollmentData enrollmentData;

    private TextView tAgentStatusEnrolled;
    private TextView tAgentStatusUnenrolled;

    private RelativeLayout.LayoutParams layoutParams;

    private String TAG = "MainActivity";

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

        btnEnrollUnenroll.setOnClickListener(view -> {
            if (isEnrolled()) {
                showUnenrollmentDialog();
            } else {
                startActivity(new Intent(MainActivity.this, EnrollmentActivity.class));
            }
        });

        // Load UI based on enrollment status from database
        AppDatabase db = AppDatabase.getDatabase(this.getApplicationContext(), "enrollment-data");
        db.enrollmentDataDAO().getEnrollmentInfo(1).observe(this, enrollmentData -> {
            updateUIBasedOnEnrollment(enrollmentData);

            btnSyncNow.setOnClickListener(view -> {
                CheckinRepository checkinRepository = CheckinRepository.getInstance(this);
                androidx.appcompat.app.AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                        .setTitle("Checking in...")
                        .setMessage("Starting checkin...")
                        .setCancelable(false)
                        .setPositiveButton("OK", /* listener = */ null)
                        .show();
                checkinRepository.checkinAgent(enrollmentData, AgentMetadata.getMetadataFromDeviceAndDB(enrollmentData.agentId, enrollmentData.hostname), this, dialog, null, this);
            });
        });

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
    }

    private boolean isEnrolled() {
        try {
            return enrollmentData.isEnrolled;
        } catch (Exception e) {
            return false;
        }
    }

    private void updateUIBasedOnEnrollment(EnrollmentData enrollmentData) {
        this.enrollmentData = enrollmentData;
        if (isEnrolled()) {
            // Check if agent is enrolled and show sync button
            btnSyncNow.setEnabled(true);

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

        } else {
            // Check if agent is enrolled and show sync button
            btnSyncNow.setEnabled(false);
            tAgentStatusUnenrolled.setText("Agent is currently unenrolled.");
            tAgentStatusUnenrolled.setVisibility(View.VISIBLE);
            tAgentStatusEnrolled.setVisibility(View.INVISIBLE);

            // Update the button text for enrollment
            btnEnrollUnenroll.setText("Enroll Agent");
            // Set color back to elastic_agent_green
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
        // Delete the enrollment data from the database
        AppDatabase db = AppDatabase.getDatabase(this.getApplicationContext(), "enrollment-data");
        AppDatabase.databaseWriteExecutor.execute(() -> {
            db.enrollmentDataDAO().delete();
        });
        // Refresh the UI
        updateUIBasedOnEnrollment(new EnrollmentData());
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
            Log.w(TAG, "Could not parse date " + dateUnformatted);
            return dateUnformatted;
        }

    }
}
