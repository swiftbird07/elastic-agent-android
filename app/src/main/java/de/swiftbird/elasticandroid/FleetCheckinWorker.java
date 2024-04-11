package de.swiftbird.elasticandroid;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A background worker that handles periodic check-in with the Fleet server.
 * It fetches the current enrollment and policy data, then uses the FleetCheckinRepository
 * to perform the check-in operation. Depending on the check-in result and policy settings,
 * it might adjust the check-in frequency or mark the agent as unhealthy.
 */
public class FleetCheckinWorker extends Worker {
    private static final ExecutorService executor = Executors.newFixedThreadPool(4);

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
        FleetCheckinRepository repository = new FleetCheckinRepository(null, null);

        // Obtain an instance of the AppDatabase
        AppDatabase db = AppDatabase.getDatabase(this.getApplicationContext());

        // Synchronously fetch the enrollment data
        FleetEnrollData enrollmentData = db.enrollmentDataDAO().getEnrollmentInfoSync(1);
        AgentMetadata agentMetadata = AgentMetadata.getMetadataFromDeviceAndDB(enrollmentData.agentId, enrollmentData.hostname);
        PolicyData policyData = db.policyDataDAO().getPolicyDataSync();
        AtomicBoolean finished = new AtomicBoolean(false);

        StatusCallback callback = success -> {
            // Create new thread to handle the callback
            executor.execute(() -> {
                try {
                    if (!success) {

                        AppStatisticsDataDAO statisticsData = db.statisticsDataDAO();
                        statisticsData.increaseTotalFailures();

                        // Use exponential backoff for the next check-in if enabled
                        if (policyData.useBackoff) {
                            // Calculate the intended backoff interval
                            int intendedBackoff = getIntendedBackoff(policyData);
                            policyData.backoffCheckinInterval = intendedBackoff;
                            db.policyDataDAO().setBackoffCheckinInterval(intendedBackoff);

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
                } catch (Exception e) {
                    AppLog.e("FleetCheckinWorker", "Unhandled app error during check-in worker: " + e.getMessage());
                }

                // Schedule the next check-in.
                // Notice that we can't use a periodic worker, as the interval is dynamic and likely also under the minimum scheduling interval of 15 minutes.
                AppLog.i("FleetCheckinWorker", "Scheduling next Fleet checkin in " + policyData.backoffCheckinInterval + " seconds");
                WorkScheduler.scheduleFleetCheckinWorker(getApplicationContext(), policyData.backoffCheckinInterval, TimeUnit.SECONDS, policyData.disableIfBatteryLow);
                finished.set(true);
            });
        };

        try {
            repository.checkinAgent(getApplicationContext(), enrollmentData, agentMetadata, callback);
        } catch (Exception e) {
            AppLog.e("FleetCheckinWorker", "Unhandled app error during check-in worker: " + e.getMessage());
            callback.onCallback(false);
        }

        return Result.success();
    }

    /**
     * Calculates the intended backoff interval based on the current policy data.
     * Normally, the backoff interval is doubled, but if a maximum backoff interval is set,
     * the minimum of the intended backoff and the maxBackoffInterval is used.
     *
     * @return The intended backoff interval in seconds.
     */
    private static int getIntendedBackoff(PolicyData policyData) {
        int intendedBackoff;

        if (policyData.maxBackoffInterval == 0) {
            intendedBackoff = policyData.backoffCheckinInterval * 2;
        } else {
            intendedBackoff = Math.min(policyData.backoffCheckinInterval * 2, policyData.maxBackoffInterval);
        }
        return intendedBackoff;
    }
}
