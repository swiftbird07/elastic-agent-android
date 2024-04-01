package de.swiftbird.elasticandroid;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

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
        int nextIntervalInSeconds = db.policyDataDAO().getPolicyDataSync().checkinInterval;
        StatusCallback callback = new StatusCallback() {
            @Override
            public void onCallback(boolean success) {
                // Schedule the next check-in
                AppLog.i("FleetCheckinWorker", "Scheduling next check-in in " + nextIntervalInSeconds + " seconds");
                WorkScheduler.scheduleFleetCheckinWorker(getApplicationContext(), nextIntervalInSeconds, TimeUnit.SECONDS);
            }
        };

        repository.checkinAgent(enrollmentData, agentMetadata, callback, null, null, getApplicationContext());

        return Result.success();
    }



}
