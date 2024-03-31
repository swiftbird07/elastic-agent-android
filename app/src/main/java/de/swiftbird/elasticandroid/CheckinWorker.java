package de.swiftbird.elasticandroid;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.util.concurrent.TimeUnit;

public class CheckinWorker extends Worker {

    public CheckinWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        Log.i("CheckinWorker", "Performing check-in from background worker");

        // Using CheckinRepository for periodic check-in
        CheckinRepository repository = CheckinRepository.getInstance(getApplicationContext());

        // Obtain an instance of the AppDatabase
        AppDatabase db = AppDatabase.getDatabase(this.getApplicationContext(), "enrollment-data");

        // Synchronously fetch the enrollment data; adjust the method call as necessary based on your DAO
        EnrollmentData enrollmentData = db.enrollmentDataDAO().getEnrollmentInfoSync(1); // Assuming you have a method like this
        AgentMetadata agentMetadata = AgentMetadata.getMetadataFromDeviceAndDB(enrollmentData.agentId, enrollmentData.hostname);
        int nextIntervalInSeconds = db.policyDataDAO().getPolicyDataSync().checkinInterval;
        StatusCallback callback = new StatusCallback() {
            @Override
            public void onCallback(boolean success) {
                // Schedule the next check-in
                Log.i("CheckinWorker", "Scheduling next check-in in " + nextIntervalInSeconds + " seconds");
                WorkScheduler.scheduleFleetCheckinWorker(getApplicationContext(), nextIntervalInSeconds, TimeUnit.SECONDS);
            }
        };

        repository.checkinAgent(enrollmentData, agentMetadata, callback, null, null, getApplicationContext());

        return Result.success();
    }



}
