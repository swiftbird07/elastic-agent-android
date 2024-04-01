package de.swiftbird.elasticandroid;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class FleetCheckinWorker extends Worker {

    public FleetCheckinWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

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
                        if(!success) {
                            AppStatisticsDataDAO statisticsData = db.statisticsDataDAO();
                            statisticsData.increaseTotalFailures();
                            // Use exponential backoff for the next check-in
                            policyData.backoffCheckinInterval = policyData.backoffCheckinInterval * 2;
                            db.policyDataDAO().increaseBackoffPutInterval();
                            AppLog.w("FleetCheckinWorker", "Fleet checkin failed, increasing interval to " + policyData.putInterval + " seconds");
                        } else {
                            // Reset the backoff interval
                            policyData.backoffCheckinInterval = policyData.checkinInterval;
                            db.policyDataDAO().resetBackoffCheckinInterval();
                        }
                        // Schedule the next check-in
                        AppLog.i("FleetCheckinWorker", "Scheduling next check-in in " + policyData.backoffCheckinInterval + " seconds");
                        WorkScheduler.scheduleElasticsearchWorker(getApplicationContext(), policyData.backoffCheckinInterval, TimeUnit.SECONDS);
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
