package de.swiftbird.elasticandroid;

import android.content.Context;

import androidx.work.Constraints;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import java.util.concurrent.TimeUnit;

public class WorkScheduler {

    protected static final String FLEET_CHECKIN_WORK_NAME = "fleet_checkin";
    protected static final String ELASTICSEARCH_PUT_WORK_NAME = "elasticsearch-put";

    public static void scheduleFleetCheckinWorker(Context context, long interval, TimeUnit timeUnit) {
        AppLog.i("WorkScheduler", "Scheduling fleet check-in worker");
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

    public static void scheduleElasticsearchWorker(Context context, long interval, TimeUnit timeUnit) {
        AppLog.i("WorkScheduler", "Scheduling Elasticsearch put worker");
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

    public static void cancelAllWork(Context context) {
        AppLog.i("WorkScheduler", "Cancelling all work");
        WorkManager.getInstance(context).cancelAllWork();
    }
}
