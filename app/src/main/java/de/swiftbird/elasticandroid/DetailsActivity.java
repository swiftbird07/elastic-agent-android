package de.swiftbird.elasticandroid;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import de.swiftbird.elasticandroid.R.id;

/**
 * Activity that presents a comprehensive view of the agent's status post-enrollment,
 * aggregating data from enrollment details, fleet policy settings, and Elasticsearch statistics.
 * It serves as a diagnostic tool for administrators to assess the agent's operational status
 * without direct log inspection. The activity auto-refreshes to provide up-to-date information.
 */
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
    private TextView useBackoffValue;
    private TextView maxBackoffValue;

    // ES Texts
    private TextView workersValue;
    private TextView esIntervalValue;
    private TextView combinedBufferSizeValue;
    private TextView lastDocumentsSendAtValue;
    private TextView lastDocumentsSendSizeValue;

    // Enrollment Details Layout
    private LinearLayout llEnrollmentDetails;
    private final String TAG = "DetailsActivity";
    private FleetEnrollData enrollmentData;
    private final Handler handler = new Handler();
    private final Map<String, String> workStatusMap = new HashMap<>();

    private final Runnable runnableCode = new Runnable() {
        @Override
        public void run() {
            update();
            // Repeat this runnable code again every 5 seconds
            // TODO: Make this interval configurable
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
        useBackoffValue = findViewById(R.id.useBackoffValue);
        maxBackoffValue = findViewById(R.id.maxBackoffValue);

        workersValue = findViewById(R.id.workersValue);
        esIntervalValue = findViewById(R.id.esIntervalValue);
        combinedBufferSizeValue = findViewById(R.id.combinedBufferSizeValue);
        lastDocumentsSendAtValue = findViewById(R.id.lastDocumentsSendAtValue);
        lastDocumentsSendSizeValue = findViewById(R.id.lastDocumentsSendSizeValue);


        tLastPolicyUpdateValue = findViewById(R.id.tLastPolicyUpdateValue);
        llEnrollmentDetails = findViewById(R.id.llEnrollmentDetails);

        setupWorkObservation();

        Button btnBack = findViewById(id.btnBack);

        btnBack.setOnClickListener(view -> finish());
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

    /**
     * Updates the UI with the latest enrollment data, adjusting visibility and content
     * based on the current enrollment status of the agent.
     * @param enrollmentData The latest enrollment data for the agent.
     */
    @SuppressLint("SetTextI18n")
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
            tAgentStatusValue.setTextColor(ContextCompat.getColor(this, android.R.color.darker_gray)); // Set text color to red if unenrolled

            // Hide the enrollment details if not enrolled
            showEnrollmentDetails(false);
        }
    }

    /**
     * Updates the UI with the latest policy data, reflecting the current policy settings
     * applied to the agent from the fleet management.
     * @param policyData The latest policy data for the agent.
     */
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

        // Backoff
        useBackoffValue.setText(policyData.useBackoff ? "Yes" : "No");
        maxBackoffValue.setText(policyData.maxBackoffInterval != 0 ? FleetCheckinRepository.secondsToTimeInterval(policyData.maxBackoffInterval) : "Not Set");
    }

    /**
     * Updates the UI with the latest Elasticsearch statistics, providing insights into
     * the agent's data reporting and buffer status.
     * @param statisticsData The latest statistics data from Elasticsearch.
     */
    private void updateUIBasedOnStatistics(AppStatisticsData statisticsData) {
        // Update the UI based on the statistics data
        lastDocumentsSendAtValue.setText(statisticsData.lastDocumentsSentAt != null ? statisticsData.lastDocumentsSentAt : "Never");
        lastDocumentsSendSizeValue.setText(statisticsData.lastDocumentsSentCount != -1 ? String.valueOf(statisticsData.lastDocumentsSentCount) : "Never");
        combinedBufferSizeValue.setText(statisticsData.combinedBufferSize != -1 ? String.valueOf(statisticsData.combinedBufferSize) : "0");
        tAgentStatusValue.setText(statisticsData.agentHealth != null ? statisticsData.agentHealth : "Unhealthy");
        tAgentStatusValue.setTextColor(ContextCompat.getColor(this, statisticsData.agentHealth != null && statisticsData.agentHealth.equals("Healthy") ? android.R.color.holo_green_dark : android.R.color.holo_orange_dark));
    }

    /**
     * Controls the visibility of enrollment details within the UI based on the agent's
     * enrollment status.
     * @param show Indicates whether to show or hide the enrollment details section.
     */
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

    /**
     * Determines if the agent is currently enrolled based on the presence of enrollment data.
     * @return True if the agent is enrolled, false otherwise.
     */
    private boolean isEnrolled() {
        try {
            return enrollmentData.isEnrolled;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Central method to refresh the UI with the latest data across all categories
     * (enrollment, policy, and statistics).
     */
    private void update(){
        // TODO: Extract this to a separate DetailsRepository class
        AppLog.d(TAG, "Updating UI..."); // TODO: Maybe display a message visible to the user indicating the update

        try {
            // Load UI based on enrollment status from database
            AppDatabase db = AppDatabase.getDatabase(this.getApplicationContext(), "");
            db.enrollmentDataDAO().getEnrollmentInfo(1).observe(this, enrollmentData -> {
                if(enrollmentData != null) {
                    updateUIBasedOnEnrollment(enrollmentData);
                }
            });

            db.policyDataDAO().getPolicyData().observe(this, policyData -> {
                if(policyData != null) {
                    updateUIBasedOnPolicy(policyData);
                }
            });

            db.statisticsDataDAO().getStatistics().observe(this, statisticsData -> {
                if(statisticsData != null) {
                    updateUIBasedOnStatistics(statisticsData);
                }
            });
        } catch (Exception e) {
            AppLog.e(TAG, "Error updating UI: " + e.getMessage());
        }
    }

    /**
     * Observes the status of work tasks related to the agent's operations, updating the UI
     * with the status and health based on the outcomes of these tasks.
     * @param workName The name of the work task to observe.
     * @param workersValue TextView to display the work status.
     * @param failing A flag indicating if any work task has failed.
     */
    private void observeWorkAndSetStatus(String workName, TextView workersValue, AtomicBoolean failing) {
        // TODO: Extract this to a separate DetailsRepository class
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

                    AppDatabase db = AppDatabase.getDatabase(this.getApplicationContext(), "");

                    Executors.newSingleThreadExecutor().execute(() -> {
                        // Perform database read operation in background
                        AppStatisticsData statisticsData = db.statisticsDataDAO().getStatisticsSync();
                        if (statisticsData != null) {
                            db.statisticsDataDAO().setAgentHealth(failing.get() ? "Unhealthy" : "Healthy");
                        }
                    });
                });
    }

    /**
     * Sets up observation for work tasks using WorkManager, updating the UI with the current
     * status and health of the agent based on work task outcomes.
     */
    private void setupWorkObservation() {
        workersValue.setText("");
        boolean failingBool = false;
        AtomicBoolean failing = new AtomicBoolean(failingBool);

        observeWorkAndSetStatus(WorkScheduler.FLEET_CHECKIN_WORK_NAME, workersValue, failing);
        observeWorkAndSetStatus(WorkScheduler.ELASTICSEARCH_PUT_WORK_NAME, workersValue, failing);
    }
}


