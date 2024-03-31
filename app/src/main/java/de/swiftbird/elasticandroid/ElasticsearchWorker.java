package de.swiftbird.elasticandroid;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

public class ElasticsearchWorker extends Worker {

    public ElasticsearchWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        // Here, you would implement the logic to send the collected events to Elasticsearch.
        // This is just a placeholder for your Elasticsearch repository logic.
        // Example: ElasticsearchRepository.getInstance(getApplicationContext()).sendEvents();

        return Result.success();
    }
}
