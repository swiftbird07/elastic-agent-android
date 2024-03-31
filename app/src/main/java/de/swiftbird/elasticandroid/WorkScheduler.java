package de.swiftbird.elasticandroid;

import android.content.Context;
import android.util.Log;

import androidx.work.Constraints;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import java.util.concurrent.TimeUnit;

public class WorkScheduler {

    protected static final String FLEET_CHECKIN_WORK_NAME = "fleet_checkin";

    public static void scheduleFleetCheckinWorker(Context context, long interval, TimeUnit timeUnit) {
        Log.i("WorkScheduler", "Scheduling fleet check-in worker");
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        OneTimeWorkRequest.Builder builder = new OneTimeWorkRequest.Builder(CheckinWorker.class)
                .setInitialDelay(interval, timeUnit)
                .setConstraints(constraints);

        OneTimeWorkRequest workRequest = builder.build();
        WorkManager.getInstance(context).enqueueUniqueWork(FLEET_CHECKIN_WORK_NAME, ExistingWorkPolicy.REPLACE, workRequest);
    }

    public static void scheduleElasticsearchWorker(Context context) {
        Log.i("WorkScheduler", "Scheduling Elasticsearch worker");
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        OneTimeWorkRequest.Builder builder = new OneTimeWorkRequest.Builder(ElasticsearchWorker.class)
                .setConstraints(constraints);

        OneTimeWorkRequest workRequest = builder.build();
        WorkManager.getInstance(context).enqueue(workRequest);
    }
}
