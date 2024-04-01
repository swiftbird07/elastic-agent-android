package de.swiftbird.elasticandroid;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ElasticWorker extends Worker {

    private static final String TAG = "ElasticWorker"; //TODO: Make this static final for every class

    public ElasticWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        AppLog.i("FleetCheckinWorker", "Performing elasticsearch PUT from background worker");

        // Obtain an instance of the AppDatabase
        AppDatabase db = AppDatabase.getDatabase(this.getApplicationContext(), "enrollment-data");

        // Synchronously fetch the enrollment data; adjust the method call as necessary based on your DAO
        FleetEnrollData enrollmentData = db.enrollmentDataDAO().getEnrollmentInfoSync(1); // Assuming you have a method like this
        AgentMetadata agentMetadata = AgentMetadata.getMetadataFromDeviceAndDB(enrollmentData.agentId, enrollmentData.hostname);
        PolicyData policyData = db.policyDataDAO().getPolicyDataSync();
        int nextIntervalInSeconds = policyData.putInterval;
        StatusCallback callback = new StatusCallback() {
            @Override
            public void onCallback(boolean success) {
                // Schedule the next check-in
                AppLog.i("FleetCheckinWorker", "Scheduling next check-in in " + nextIntervalInSeconds + " seconds");
                WorkScheduler.scheduleElasticsearchWorker(getApplicationContext(), nextIntervalInSeconds, TimeUnit.SECONDS);
            }
        };

        ArrayList<ElasticDocument> newDocuments = new ArrayList<>();

        for(String componentPath : policyData.paths.split(",")) {
            Component component = ComponentFactory.createInstance(componentPath);
            newDocuments.addAll(component.getDocumentsFromBuffer(policyData.maxDocumentsPerRequest));
        }

        // Make a PUT request to Elasticsearch using Retrofit
        String sslFingerprint = policyData.sslCaTrustedFingerprint;
        String esUrl = policyData.hosts;
        boolean verifyCert = true; // we use the fingerprint anyway
        String elasticAccessApiKey = policyData.apiKey;
        String indexName = policyData.dataStreamDataset;

        // Initialize Retrofit instance
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(esUrl)
                .client(NetworkBuilder.getOkHttpClient(verifyCert)) // TODO: Use SSL fingerprint verification
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        ElasticApi elasticApi = retrofit.create(ElasticApi.class);

        elasticApi.putBulk("ApiKey " + elasticAccessApiKey, indexName, newDocuments).enqueue(new Callback<ElasticResponse>() {
            @Override
            public void onResponse(@NonNull Call<ElasticResponse> call, @NonNull Response<ElasticResponse> response) {
                AppLog.d(TAG, "Got Response from Fleet Server: " + new Gson().toJson(response.body()));
                if (response.isSuccessful()) {
                    AppLog.i(TAG, "Elasticsearch PUT successful");
                    callback.onCallback(true);
                } else {
                    AppLog.w(TAG, "Elasticsearch PUT failed: " + response.message());
                    callback.onCallback(false);
                }
            }

            @Override
            public void onFailure(@NonNull Call<ElasticResponse> call, @NonNull Throwable t) {
                AppLog.w(TAG, "Elasticsearch PUT failed: " + t.getMessage());
                callback.onCallback(false);
            }

        });




        return Result.success();
    }
}
