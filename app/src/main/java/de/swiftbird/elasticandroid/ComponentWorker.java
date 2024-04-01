package de.swiftbird.elasticandroid;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

public class ComponentWorker extends Worker {
    public ComponentWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        // TODO: Implement usage of this worker^
    }

    @NonNull
    @Override
    public Result doWork() {
        AppLog.i("ComponentWorker", "Performing component worker tasks in the background");

        // For every available component, perform the necessary tasks
        FleetEnrollData enrollmentData = AppDatabase.getDatabase(getApplicationContext(), "enrollment-data").enrollmentDataDAO().getEnrollmentInfoSync(1);
        PolicyData policyData = AppDatabase.getDatabase(getApplicationContext(), "enrollment-data").policyDataDAO().getPolicyDataSync();

        for(String componentPath : policyData.paths.split(",")) {
            Component component = ComponentFactory.createInstance(componentPath);
            component.setup(getApplicationContext(), enrollmentData, policyData);
            component.collectEvents(enrollmentData, policyData);
        }

        return Result.success();
    }
}


