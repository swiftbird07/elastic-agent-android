package de.swiftbird.elasticandroid;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

/**
 * A worker class that extends WorkManager's Worker, designed to execute background tasks for various components of the application.
 * It aims to perform operations that should run outside of the application's UI thread for collecting events.
 * This worker iterates over all components specified in the application's policy data, initializes them,
 * and triggers their event collection routines.
 *
 * <p></p>Note: This class is part of the application's background execution strategy but is not yet integrated into the application's workflow.
 * TODO: Integrate this worker into the application's workflow to enable background data collection for components.
 */
public class ComponentWorker extends Worker {
    /**
     * Constructor initializing the worker with application context and worker parameters.
     *
     * @param context The application context.
     * @param workerParams Parameters for configuring the worker, including input data and tags.
     */
    public ComponentWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        // Future implementation will use parameters to configure worker tasks.
    }

    /**
     * Executes the background task. This is where the worker iterates over available components,
     * performs setup and triggers data collection based on the current policy data.
     *
     * @return The result of the background work; {@link Result#success()} if the operation completes successfully.
     */
    @NonNull
    @Override
    public Result doWork() {
        AppLog.i("ComponentWorker", "Performing component worker tasks in the background");

        // Retrieves enrollment and policy data to configure and operate on components.
        FleetEnrollData enrollmentData = AppDatabase.getDatabase(getApplicationContext()).enrollmentDataDAO().getEnrollmentInfoSync(1);
        PolicyData policyData = AppDatabase.getDatabase(getApplicationContext()).policyDataDAO().getPolicyDataSync();

        // Iterates over components defined in policy data, initializing and collecting events for each.
        for(String componentPath : policyData.paths.split(",")) {
            try {
                if (!componentPath.startsWith("android://")) {
                    AppLog.w("ComponentWorker", "Invalid component path: " + componentPath);
                    continue;
                }
                Component component = ComponentFactory.createInstance(componentPath);
                component.setup(getApplicationContext(), enrollmentData, policyData, "");
                component.collectEvents(enrollmentData, policyData);

            } catch (Exception e) {
                AppLog.e("ComponentWorker", "Error processing component: " + componentPath);
            }
        }
        return Result.success(); // Indicates successful completion of worker tasks.
    }
}
