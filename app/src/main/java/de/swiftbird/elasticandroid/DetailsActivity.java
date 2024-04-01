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

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

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

    // ES Texts

    private TextView workersValue;
    private TextView esIntervalValue;
    private TextView combinedBufferSizeValue;
    private TextView lastDocumentsSendAtValue;
    private TextView lastDocumentsSendSizeValue;

    // Linear Layouts

    private LinearLayout llEnrollmentDetails;
    private LinearLayout llPolicyDetails;
    private Button btnBack;

    private String TAG = "DetailsActivity";

    private FleetEnrollData enrollmentData;

    private Handler handler = new Handler();

    private boolean failingBool = false;

    private final Map<String, String> workStatusMap = new HashMap<>();

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
        esIntervalValue = findViewById(R.id.esIntervalValue);
        combinedBufferSizeValue = findViewById(R.id.combinedBufferSizeValue);
        lastDocumentsSendAtValue = findViewById(R.id.lastDocumentsSendAtValue);
        lastDocumentsSendSizeValue = findViewById(R.id.lastDocumentsSendSizeValue);


        tLastPolicyUpdateValue = findViewById(R.id.tLastPolicyUpdateValue);
        llEnrollmentDetails = findViewById(R.id.llEnrollmentDetails);

        setupWorkObservation();

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

    private void updateUIBasedOnEnrollment(FleetEnrollData enrollmentData) {
        this.enrollmentData = enrollmentData;
        if (isEnrolled()) {
            // Update the agent status and other TextViews with data from FleetEnrollData object
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
        esUrlValue.setText(policyData.hosts != null ? policyData.hosts : "Not Set");
        esSslFingerprintValue.setText(policyData.sslCaTrustedFingerprint != null ? policyData.sslCaTrustedFingerprint : "Not Set");

        if(policyData.backoffPutInterval != policyData.putInterval){
            esIntervalValue.setText(policyData.putInterval != -1 ? FleetCheckinRepository.secondsToTimeInterval(policyData.putInterval) + " (Backoff: " + FleetCheckinRepository.secondsToTimeInterval(policyData.backoffPutInterval) + ")" : "Not Set");
        } else {
        esIntervalValue.setText(policyData.putInterval != -1 ? FleetCheckinRepository.secondsToTimeInterval(policyData.putInterval) : "Not Set");
        }

        if(policyData.backoffCheckinInterval != policyData.checkinInterval){
            checkinIntervalValue.setText(policyData.checkinInterval != -1 ? FleetCheckinRepository.secondsToTimeInterval(policyData.checkinInterval) + " (Backoff: " + FleetCheckinRepository.secondsToTimeInterval(policyData.backoffCheckinInterval) + ")" : "Not Set");
        } else {
        checkinIntervalValue.setText(policyData.checkinInterval != -1 ? FleetCheckinRepository.secondsToTimeInterval(policyData.checkinInterval) : "Not Set");
        }
    }

    private void updateUIBasedOnStatistics(AppStatisticsData statisticsData) {
        // Update the UI based on the statistics data
        lastDocumentsSendAtValue.setText(statisticsData.lastDocumentsSentAt != null ? statisticsData.lastDocumentsSentAt : "Never");
        lastDocumentsSendSizeValue.setText(statisticsData.lastDocumentsSentCount != -1 ? String.valueOf(statisticsData.lastDocumentsSentCount) : "Never");
        combinedBufferSizeValue.setText(statisticsData.combinedBufferSize != -1 ? String.valueOf(statisticsData.combinedBufferSize) : "0");
        tAgentStatusValue.setText(statisticsData.agentHealth != null ? statisticsData.agentHealth : "Unhealthy");
        tAgentStatusValue.setTextColor(getResources().getColor(statisticsData.agentHealth != null && statisticsData.agentHealth.equals("Healthy") ? android.R.color.holo_green_dark : android.R.color.holo_orange_dark));
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
        lastDocumentsSendAtValue.setVisibility(visibility);
        lastDocumentsSendSizeValue.setVisibility(visibility);
        combinedBufferSizeValue.setVisibility(visibility);
    }

    private boolean isEnrolled() {
        try {
            return enrollmentData.isEnrolled;
        } catch (Exception e) {
            return false;
        }
    }

    private void update(){
        AppLog.d(TAG, "Updating UI...");

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

        db.statisticsDataDAO().getStatistics().observe(this, statisticsData -> {
            if(statisticsData != null) {
                updateUIBasedOnStatistics(statisticsData);
            }
        });


    }

    private void observeWorkAndSetStatus(String workName, TextView workersValue, AtomicBoolean failing) {
        WorkManager.getInstance(getApplicationContext())
                .getWorkInfosByTagLiveData(workName)
                .observe(this, workInfos -> {
                    for (WorkInfo workInfo : workInfos) {
                        // Log the current state
                        AppLog.d(TAG, "Work with tag " + workName + " is in state " + workInfo.getState());

                        // Update the map with the latest state
                        workStatusMap.put(workName, workInfo.getState().toString());

                        // Check if any work has failed
                        if (workInfo.getState() == WorkInfo.State.FAILED) {
                            AppLog.w(TAG, "Work with tag " + workName + " failed");
                            failing.set(true);
                        }
                    }

                    // Rebuild the status string
                    StringBuilder statusBuilder = new StringBuilder();
                    for (Map.Entry<String, String> entry : workStatusMap.entrySet()) {
                        statusBuilder.append(entry.getKey()).append(" is in state ").append(entry.getValue()).append("\n");
                    }

                    // Update the TextView
                    workersValue.setText(statusBuilder.toString());

                    AppDatabase db = AppDatabase.getDatabase(this.getApplicationContext(), "enrollment-data");

                    Executors.newSingleThreadExecutor().execute(() -> {
                        // Perform database read operation in background
                        AppStatisticsData statisticsData = db.statisticsDataDAO().getStatisticsSync();
                        if (statisticsData != null) {
                            db.statisticsDataDAO().setAgentHealth(failing.get() ? "Unhealthy" : "Healthy");
                        }
                    });
                });
    }


    private void setupWorkObservation() {
        workersValue.setText("");
        AtomicBoolean failing = new AtomicBoolean(failingBool);

        observeWorkAndSetStatus(WorkScheduler.FLEET_CHECKIN_WORK_NAME, workersValue, failing);
        observeWorkAndSetStatus(WorkScheduler.ELASTICSEARCH_PUT_WORK_NAME, workersValue, failing);
    }

}


