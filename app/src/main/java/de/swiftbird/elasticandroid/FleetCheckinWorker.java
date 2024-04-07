package de.swiftbird.elasticandroid;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * A background worker that handles periodic check-in with the Fleet server.
 * It fetches the current enrollment and policy data, then uses the FleetCheckinRepository
 * to perform the check-in operation. Depending on the check-in result and policy settings,
 * it might adjust the check-in frequency or mark the agent as unhealthy.
 */
public class FleetCheckinWorker extends Worker {

    /**
     * Initializes a new instance of the FleetCheckinWorker.
     *
     * @param context Application context.
     * @param workerParams Parameters for the work.
     */
    public FleetCheckinWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    /**
     * Performs the check-in operation asynchronously.
     *
     * @return The result of the work, indicating success or retry requirements.
     */
    @NonNull
    @Override
    public Result doWork() {
        AppLog.i("FleetCheckinWorker", "Performing check-in from background worker");

        // Using FleetCheckinRepository for periodic check-in
        FleetCheckinRepository repository = FleetCheckinRepository.getInstance(getApplicationContext());

        // Obtain an instance of the AppDatabase
        AppDatabase db = AppDatabase.getDatabase(this.getApplicationContext(), "enrollment-data");

        // Synchronously fetch the enrollment data; adjust the method call as necessary based on your DAO
        FleetEnrollData enrollmentData = db.enrollmentDataDAO().getEnrollmentInfoSync(1); // Assuming you have a method like this
        AgentMetadata agentMetadata = AgentMetadata.getMetadataFromDeviceAndDB(enrollmentData.agentId, enrollmentData.hostname);
        PolicyData policyData = db.policyDataDAO().getPolicyDataSync();
        int nextIntervalInSeconds = db.policyDataDAO().getPolicyDataSync().checkinInterval;

        StatusCallback callback = new StatusCallback() {
            @Override
            public void onCallback(boolean success) {
                // Create new thread to handle the callback
                Executors.newSingleThreadExecutor().execute(() -> {
                    if (!success) {
                        AppStatisticsDataDAO statisticsData = db.statisticsDataDAO();
                        statisticsData.increaseTotalFailures();

                        // Use exponential backoff for the next check-in if enanbled
                        if (policyData.useBackoff) {
                            int intendedBackoff = policyData.backoffCheckinInterval * 2;

                            // Use old backoff interval if the current interval is greater than the max interval
                            if (intendedBackoff > policyData.maxBackoffInterval) {
                                policyData.backoffCheckinInterval = policyData.maxBackoffInterval;
                                db.policyDataDAO().setBackoffCheckinInterval(policyData.maxBackoffInterval);
                            } else {
                                // Configure the new backoff interval
                                policyData.backoffCheckinInterval = intendedBackoff;
                                db.policyDataDAO().setBackoffCheckinInterval(intendedBackoff);
                            }

                            // Set agent health status to unhealthy
                            db.statisticsDataDAO().setAgentHealth("Unhealthy");
                            AppLog.w("FleetCheckinWorker", "Fleet checkin failed, increasing interval to " + policyData.backoffCheckinInterval + " seconds");
                        }
                    } else {
                        // If Elasticsearch PUT also succeeded, reset the agent health status
                        if (policyData.backoffPutInterval == policyData.putInterval) {
                            db.statisticsDataDAO().setAgentHealth("Healthy");
                        }
                        // Reset the backoff interval
                        policyData.backoffCheckinInterval = policyData.checkinInterval;
                        db.policyDataDAO().resetBackoffCheckinInterval();
                    }

                    // Schedule the next check-in
                    AppLog.i("FleetCheckinWorker", "Scheduling next Fleet checkin in " + policyData.backoffCheckinInterval + " seconds");
                    WorkScheduler.scheduleFleetCheckinWorker(getApplicationContext(), policyData.backoffCheckinInterval, TimeUnit.SECONDS);
                });

            }
        };


        try {
            repository.checkinAgent(enrollmentData, agentMetadata, callback, null, null, getApplicationContext());
        } catch (Exception e) {
            AppLog.e("FleetCheckinWorker", "Unhandled app error during check-in worker: " + e.getMessage());
            callback.onCallback(false);
        }

        return Result.success();
    }
}
