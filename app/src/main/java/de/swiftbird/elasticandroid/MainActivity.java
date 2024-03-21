package de.swiftbird.elasticandroid;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;

import com.google.android.material.snackbar.Snackbar;

import androidx.appcompat.app.AppCompatActivity;

import android.view.View;

import androidx.core.content.ContextCompat;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import de.swiftbird.elasticandroid.databinding.ActivityMainBinding;

import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {

    private TextView tAgentStatusValue;
    private TextView tHostnameValue;
    private TextView tPolicyValue;
    private TextView tPolicyIdValue;
    private TextView tEnrolledAtValue;
    private TextView tLastCheckinValue;
    private TextView tLastPolicyUpdateValue;
    private Button btnEnrollUnenroll;
    private LinearLayout llEnrollmentDetails;

    private EnrollmentData enrollmentData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize TextView and Button properties
        tAgentStatusValue = findViewById(R.id.tAgentStatusValue);
        tHostnameValue = findViewById(R.id.tHostnameValue);
        tPolicyValue = findViewById(R.id.tPolicyValue);
        tPolicyIdValue = findViewById(R.id.tPolicyIdValue);
        tEnrolledAtValue = findViewById(R.id.tEnrolledAtValue);
        tLastCheckinValue = findViewById(R.id.tLastCheckinValue);
        tLastPolicyUpdateValue = findViewById(R.id.tLastPolicyUpdateValue);
        btnEnrollUnenroll = findViewById(R.id.btnEnrollUnenroll);
        llEnrollmentDetails = findViewById(R.id.llEnrollmentDetails);

        btnEnrollUnenroll.setOnClickListener(view -> {
            if (isEnrolled()) {
                showUnenrollmentDialog();
            } else {
                startActivity(new Intent(MainActivity.this, EnrollmentActivity.class));
            }
        });

        // Load UI based on enrollment status from database
        AppDatabase db = AppDatabase.getDatabase(this.getApplicationContext());
        db.enrollmentDataDAO().getEnrollmentInfo(1).observe(this, enrollmentData -> {
            updateUIBasedOnEnrollment(enrollmentData);
        });

    }

    @Override
    protected void onResume() {
        super.onResume();
        // Load UI based on enrollment status from database
        AppDatabase db = AppDatabase.getDatabase(this.getApplicationContext());
        db.enrollmentDataDAO().getEnrollmentInfo(1).observe(this, enrollmentData -> {
            updateUIBasedOnEnrollment(enrollmentData);
        });
    }

    private boolean isEnrolled() {
        // Check enrollment status (for now, assume false)
        try {
            return enrollmentData.isEnrolled;
        } catch (Exception e) {
            return false;
        }
    }

    private void updateUIBasedOnEnrollment(EnrollmentData enrollmentData) {
        this.enrollmentData = enrollmentData;
        if (isEnrolled()) {

            // Update the agent status and other TextViews with data from EnrollmentData object
            tAgentStatusValue.setText(enrollmentData.isEnrolled ? "Enrolled" : "Unenrolled");
            tAgentStatusValue.setTextColor(getResources().getColor(android.R.color.holo_green_dark)); // Set text color to green if enrolled
            llEnrollmentDetails.setVisibility(View.VISIBLE);

            // Assuming you have setters or public fields in EnrollmentData for these details
            tHostnameValue.setText( (enrollmentData.hostname != null ? enrollmentData.hostname : "N/A"));
            tPolicyValue.setText( (enrollmentData.action != null ? enrollmentData.action : "N/A"));
            tPolicyIdValue.setText( (enrollmentData.policyId != null ? enrollmentData.policyId : "N/A"));
            tEnrolledAtValue.setText( (enrollmentData.enrolledAt != null ? enrollmentData.enrolledAt : "N/A"));
            tLastCheckinValue.setText("Placeholder"); // TODO: Update this with actual last check-in data when available
            tLastPolicyUpdateValue.setText("Placeholder"); // TODO: Update this with actual last policy update data when available

            // Update the button text for unenrollment
            btnEnrollUnenroll.setText("Unenroll Agent");
            btnEnrollUnenroll.setBackgroundColor(getResources().getColor(android.R.color.holo_red_light));
            btnEnrollUnenroll.setTextColor(getResources().getColor(android.R.color.white));

            showEnrollmentDetails(true);

        } else {
            tAgentStatusValue.setText("Unenrolled");
            tAgentStatusValue.setTextColor(getResources().getColor(android.R.color.holo_red_light)); // Set text color to red if unenrolled

            // Update the button text for enrollment
            btnEnrollUnenroll.setText("Enroll Agent");
            // Set color back to elastic_agent_green
            btnEnrollUnenroll.setBackgroundColor(ContextCompat.getColor(this, R.color.elastic_agent_green));


            // Hide the enrollment details if not enrolled
            showEnrollmentDetails(false);
        }
    }

    private void showEnrollmentDetails(boolean show) {
        // If using View visibility to show/hide enrollment details, implement logic here
        int visibility = show ? View.VISIBLE : View.GONE;
        tHostnameValue.setVisibility(visibility);
        tPolicyValue.setVisibility(visibility);
        tPolicyIdValue.setVisibility(visibility);
        tEnrolledAtValue.setVisibility(visibility);
        tLastCheckinValue.setVisibility(visibility);
        tLastPolicyUpdateValue.setVisibility(visibility);
    }

    private void clearEnrollmentDetails() {
        // Clear the TextViews or set to "N/A" when not enrolled
        tHostnameValue.setText("Hostname: N/A");
        tPolicyValue.setText("Agent Policy: N/A");
        tPolicyIdValue.setText("Policy ID: N/A");
        tEnrolledAtValue.setText("Enrolled At: N/A");
        tLastCheckinValue.setText("Last Checkin: N/A");
        tLastPolicyUpdateValue.setText("Last Policy Update: N/A");
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
        AppDatabase db = AppDatabase.getDatabase(this.getApplicationContext());
        AppDatabase.databaseWriteExecutor.execute(() -> {
            db.enrollmentDataDAO().delete();
        });
        // Refresh the UI
        updateUIBasedOnEnrollment(new EnrollmentData());
    }
}
