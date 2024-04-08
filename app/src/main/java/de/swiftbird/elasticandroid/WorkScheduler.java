package de.swiftbird.elasticandroid;

import android.content.Context;

import androidx.work.Constraints;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import java.util.concurrent.TimeUnit;

/**
 * Manages the scheduling of background tasks related to fleet check-in and Elasticsearch data transmission within the Elastic Android application.
 * Utilizes the Android WorkManager API to schedule and manage these tasks under specified constraints, such as network availability.
 *
 * <p>Both tasks are crucial for maintaining the agent's operational status and ensuring data consistency and availability for analytics and monitoring purposes.</p>
 *
 * <p>Tasks are scheduled as unique, one-time work requests with a defined initial delay and network connectivity requirement. This approach ensures that tasks are executed in an efficient manner, respecting device constraints and optimizing for battery life.</p>
 * <p>The scheduler also provides the ability to cancel all scheduled tasks, offering control over task execution and resource management.</p>
 */
public class WorkScheduler {

    protected static final String FLEET_CHECKIN_WORK_NAME = "fleet_checkin";
    protected static final String ELASTICSEARCH_PUT_WORK_NAME = "elasticsearch-put";

    /**
     * Schedules a one-time fleet check-in work task with a specified delay and under network connectivity constraints.
     * This task is crucial for maintaining the agent's communication with the fleet server, allowing it to report its status and receive commands.
     *
     * @param context   The application context, used to access the WorkManager instance.
     * @param interval  The delay before the task is executed, specified in the units provided by the {@code timeUnit} parameter.
     * @param timeUnit  The time unit for the {@code interval} parameter, e.g., {@link TimeUnit#MINUTES}.
     */
    public static void scheduleFleetCheckinWorker(Context context, long interval, TimeUnit timeUnit) {
        AppLog.i("WorkScheduler", "Scheduling fleet check-in worker with interval " + interval + " " + timeUnit.toString());
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        OneTimeWorkRequest.Builder builder = new OneTimeWorkRequest.Builder(FleetCheckinWorker.class)
                .setInitialDelay(interval, timeUnit)
                .setConstraints(constraints)
                .addTag(FLEET_CHECKIN_WORK_NAME);

        OneTimeWorkRequest workRequest = builder.build();
        WorkManager.getInstance(context).enqueueUniqueWork(FLEET_CHECKIN_WORK_NAME, ExistingWorkPolicy.REPLACE, workRequest);
    }

    /**
     * Schedules a one-time Elasticsearch document upload work task with a specified delay and under network connectivity constraints.
     * This task enables the application to transmit stored data to Elasticsearch, supporting data analysis and monitoring efforts.
     *
     * @param context   The application context, used to access the WorkManager instance.
     * @param interval  The delay before the task is executed, specified in the units provided by the {@code timeUnit} parameter.
     * @param timeUnit  The time unit for the {@code interval} parameter, e.g., {@link TimeUnit#MINUTES}.
     */
    public static void scheduleElasticsearchWorker(Context context, long interval, TimeUnit timeUnit) {
        AppLog.i("WorkScheduler", "Scheduling Elasticsearch put worker with interval " + interval + " " + timeUnit.toString());
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        OneTimeWorkRequest.Builder builder = new OneTimeWorkRequest.Builder(ElasticWorker.class)
                .setInitialDelay(interval, timeUnit)
                .setConstraints(constraints)
                .addTag(ELASTICSEARCH_PUT_WORK_NAME);

        OneTimeWorkRequest workRequest = builder.build();
        WorkManager.getInstance(context).enqueueUniqueWork(ELASTICSEARCH_PUT_WORK_NAME, ExistingWorkPolicy.REPLACE, workRequest);
    }

    /**
     * Cancels all work tasks scheduled by the WorkManager.
     * This method provides a way to halt all scheduled background tasks, useful in scenarios requiring a cessation of activity, such as unenrollment or when resetting the work schedule.
     *
     * @param context The application context, used to access the WorkManager instance.
     */
    public static void cancelAllWork(Context context) {
        AppLog.i("WorkScheduler", "Cancelling all work");
        WorkManager.getInstance(context).cancelAllWork();
    }
}
