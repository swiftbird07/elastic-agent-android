package de.swiftbird.elasticandroid;

import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;

import de.swiftbird.elasticandroid.R.id;

public class DetailsActivity extends AppCompatActivity  {

    // Enrollment Texts
    private TextView tAgentStatusValue;
    private TextView tHostnameValue;
    private TextView tPolicyValue;
    private TextView tPolicyIdValue;
    private TextView tEnrolledAtValue;
    private TextView tLastCheckinValue;
    private TextView tLastPolicyUpdateValue;

    // Policy Texts

   private TextView fleetStatus;
    private TextView enabledCollectors;
    private TextView revisionValue;
    private TextView protectionsEnabledValue;
    private TextView inputNameValue;
    private TextView dataStreamValue;
    private TextView ignoreOlderValue;
    private TextView checkinIntervalValue;
    private TextView esUrlValue;
    private TextView esSslFingerprintValue;

    private TextView workersValue;
    private LinearLayout llEnrollmentDetails;
    private LinearLayout llPolicyDetails;
    private Button btnBack;

    private String TAG = "DetailsActivity";

    private EnrollmentData enrollmentData;

    private Handler handler = new Handler();
    private Runnable runnableCode = new Runnable() {
        @Override
        public void run() {
            update();
            // Repeat this runnable code again every 5 seconds
            handler.postDelayed(this, 5000);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_details);


        // Initialize TextView and Button properties
        tAgentStatusValue = findViewById(R.id.tAgentStatusValue);
        tHostnameValue = findViewById(R.id.tHostnameValue);
        tPolicyValue = findViewById(R.id.tPolicyValue);
        tPolicyIdValue = findViewById(R.id.tPolicyIdValue);
        tEnrolledAtValue = findViewById(R.id.tEnrolledAtValue);
        tLastCheckinValue = findViewById(R.id.tLastCheckinValue);

        fleetStatus = findViewById(id.fleetStatus);
        enabledCollectors = findViewById(id.enabledCollectors);
        revisionValue = findViewById(R.id.revisionValue);
        protectionsEnabledValue = findViewById(R.id.protectionsEnabledValue);
        inputNameValue = findViewById(R.id.inputNameValue);
        dataStreamValue = findViewById(R.id.dataStreamValue);
        ignoreOlderValue = findViewById(R.id.ignoreOlderValue);
        checkinIntervalValue = findViewById(R.id.intervalValue);
        esUrlValue = findViewById(R.id.esUrlValue);
        esSslFingerprintValue = findViewById(R.id.esSslFingerprintValue);
        workersValue = findViewById(R.id.workersValue);

        tLastPolicyUpdateValue = findViewById(R.id.tLastPolicyUpdateValue);
        llEnrollmentDetails = findViewById(R.id.llEnrollmentDetails);

        btnBack = findViewById(id.btnBack);

        btnBack.setOnClickListener(view -> {
            finish();
        });

    }

    @Override
    protected void onResume() {
        super.onResume();
        update();
        handler.post(runnableCode);
    }

    @Override
    protected void onPause(){
        super.onPause();
        // Remove callbacks to avoid memory leaks
        handler.removeCallbacks(runnableCode);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Remove callbacks to avoid memory leaks
        handler.removeCallbacks(runnableCode);
    }

    private void updateUIBasedOnEnrollment(EnrollmentData enrollmentData) {
        this.enrollmentData = enrollmentData;
        if (isEnrolled()) {
            // Update the agent status and other TextViews with data from EnrollmentData object
            tAgentStatusValue.setText(enrollmentData.isEnrolled ? "Healthy" : "N/A");
            tAgentStatusValue.setTextColor(getResources().getColor(android.R.color.holo_green_dark)); // Set text color to green if enrolled
            llEnrollmentDetails.setVisibility(View.VISIBLE);

            tHostnameValue.setText( (enrollmentData.hostname != null ? enrollmentData.hostname : "N/A"));
            tPolicyValue.setText( (enrollmentData.action != null ? enrollmentData.action : "N/A"));
            tPolicyIdValue.setText( (enrollmentData.policyId != null ? enrollmentData.policyId : "N/A"));
            tEnrolledAtValue.setText( (enrollmentData.enrolledAt != null ? enrollmentData.enrolledAt : "Never"));

            showEnrollmentDetails(true);

        } else {
            // Check if agent is enrolled and show sync button
            tAgentStatusValue.setText("Ready for Enrollment");
            tAgentStatusValue.setTextColor(getResources().getColor(android.R.color.darker_gray)); // Set text color to red if unenrolled

            // Hide the enrollment details if not enrolled
            showEnrollmentDetails(false);
        }
    }

    private void updateUIBasedOnPolicy(PolicyData policyData) {
        // Update the UI based on the policy data
        tLastCheckinValue.setText(policyData.lastUpdated != null ? policyData.lastUpdated : "Never");
        tLastPolicyUpdateValue.setText(policyData.createdAt != null ? policyData.createdAt : "Never");
        fleetStatus.setText(policyData.createdAt != null ? "Enrolled" : "Unenrolled / Unmanaged");

        enabledCollectors.setText(policyData.paths != null ? policyData.paths : "None");
        revisionValue.setText(String.valueOf(policyData.revision));
        protectionsEnabledValue.setText(policyData.protectionEnabled ? "Yes" : "No");
        inputNameValue.setText(policyData.inputName != null ? policyData.inputName : "Not Set");
        dataStreamValue.setText(policyData.dataStreamDataset != null ? policyData.dataStreamDataset : "Not Set");
        ignoreOlderValue.setText(policyData.ignoreOlder != null ? policyData.ignoreOlder : "Not Set");
        checkinIntervalValue.setText(policyData.checkinInterval != -1 ? String.valueOf(policyData.checkinInterval) : "Not Set");
        esUrlValue.setText(policyData.hosts != null ? policyData.hosts : "Not Set");
        esSslFingerprintValue.setText(policyData.sslCaTrustedFingerprint != null ? policyData.sslCaTrustedFingerprint : "Not Set");
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

    private boolean isEnrolled() {
        try {
            return enrollmentData.isEnrolled;
        } catch (Exception e) {
            return false;
        }
    }

    private void update(){
        Log.d(TAG, "Updating UI...");

        // Load UI based on enrollment status from database
        AppDatabase db = AppDatabase.getDatabase(this.getApplicationContext(), "enrollment-data");
        db.enrollmentDataDAO().getEnrollmentInfo(1).observe(this, enrollmentData -> {
            if(enrollmentData != null) {
                updateUIBasedOnEnrollment(enrollmentData);
            }
        });

        db.policyDataDAO().getPoliyData().observe(this, policyData -> {
            if(policyData != null) {
                updateUIBasedOnPolicy(policyData);
            }
        });

        // Observe work with the specified tag
        WorkManager.getInstance(getApplicationContext())
                .getWorkInfosByTagLiveData(WorkScheduler.FLEET_CHECKIN_WORK_NAME)
                .observe(this, workInfos -> {
                    for (WorkInfo workInfo : workInfos) {
                        // Log or display information about the work status
                        Log.d(TAG, "Work with tag " + WorkScheduler.FLEET_CHECKIN_WORK_NAME + " is in state " + workInfo.getState());
                        workersValue.append(WorkScheduler.FLEET_CHECKIN_WORK_NAME + " is in state " + workInfo.getState() + "\n");
                    }
                });
        /*
        WorkManager.getInstance(getApplicationContext())
                .getWorkInfosByTagLiveData(WorkScheduler.ELASTICSEARCH_WORK_NAME)
                .observe(this, workInfos -> {
                    for (WorkInfo workInfo : workInfos) {
                        // Log or display information about the work status

                        Log.d(TAG, "Work with tag " + WorkScheduler.ELASTICSEARCH_WORK_NAME + " is in state " + workInfo.getState());
                        workersValue.append(WorkScheduler.ELASTICSEARCH_WORK_NAME + " is in state " + workInfo.getState() + "\n");
                    }
                });

         */
    }

}