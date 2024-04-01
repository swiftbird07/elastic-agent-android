package de.swiftbird.elasticandroid;

import android.annotation.SuppressLint;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.google.gson.Gson;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;
import java.util.concurrent.Executors;
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
        AppStatisticsData statisticsData = db.statisticsDataDAO().getStatisticsSync();
        StatusCallback callback = new StatusCallback() {
            @Override
            public void onCallback(boolean success) {
                // Create new thread to handle the callback
                Executors.newSingleThreadExecutor().execute(() -> {
                    if(!success) {
                        AppStatisticsDataDAO statisticsData = db.statisticsDataDAO();
                        statisticsData.increaseTotalFailures();
                        // Use exponential backoff for the next check-in
                        policyData.backoffPutInterval = policyData.backoffPutInterval * 2;
                        db.policyDataDAO().increaseBackoffPutInterval();

                        // Set agent health status to unhealthy
                        db.statisticsDataDAO().setAgentHealth("Unhealthy");

                        AppLog.w("ElasticWorker", "Elasticsearch PUT failed, increasing interval to " + policyData.backoffPutInterval + " seconds");
                    } else {
                        // Reset the backoff interval
                        db.statisticsDataDAO().setAgentHealth("Healthy");
                        policyData.backoffPutInterval = policyData.putInterval;
                        db.policyDataDAO().resetBackoffPutInterval();
                    }
                    // Schedule the next check-in
                    AppLog.i("ElasticWorker", "Scheduling next Elasticsearch PUT in " + policyData.backoffPutInterval + " seconds");
                    WorkScheduler.scheduleElasticsearchWorker(getApplicationContext(), policyData.backoffPutInterval, TimeUnit.SECONDS);
                });
            }
        };


        try{
            ArrayList<ElasticDocument> newDocuments = new ArrayList<>();

            for (String componentPath : policyData.paths.split(",")) {
                try{
                    // Remove everything behind the first "." to get the component name
                    String componentName = componentPath.split("\\.")[0];

                    Component component = ComponentFactory.createInstance(componentName);
                    component.setup(getApplicationContext(), enrollmentData, policyData);
                    newDocuments.addAll(component.getDocumentsFromBuffer(policyData.maxDocumentsPerRequest));
                } catch (Exception e) {
                    AppLog.e(TAG, "Error while processing component: " + e.getMessage());
                }
            }

            AppStatisticsDataDAO statisticsDataDAO = db.statisticsDataDAO();
            statisticsDataDAO.decreaseCombinedBufferSize(newDocuments.size());

            // Make a PUT request to Elasticsearch using Retrofit
            String sslFingerprint = policyData.sslCaTrustedFingerprint;
            String esUrl = policyData.hosts;
            boolean verifyCert = true; // we use the fingerprint anyway
            String elasticAccessApiKey = policyData.apiKey;
            String indexName = policyData.dataStreamDataset;

            // Initialize Retrofit instance
            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl(esUrl)
                    .client(NetworkBuilder.getOkHttpClient(verifyCert, sslFingerprint)) // TODO: Use SSL fingerprint verification
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();

            ElasticApi elasticApi = retrofit.create(ElasticApi.class);
            elasticApi.putBulk("ApiKey " + elasticAccessApiKey, indexName, newDocuments).enqueue(new Callback<ElasticResponse>() {
                @Override
                public void onResponse(@NonNull Call<ElasticResponse> call, @NonNull Response<ElasticResponse> response) {
                    Executors.newSingleThreadExecutor().execute(() -> {
                        if (response.isSuccessful()) {
                            AppLog.i(TAG, "Elasticsearch PUT successful");

                            // Set statistics data
                            @SuppressLint("SimpleDateFormat") SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
                            sdf.setTimeZone(TimeZone.getTimeZone("UTC")); // Ensure the time is in UTC
                            statisticsDataDAO.setLastDocumentsSentAt(sdf.format(Calendar.getInstance().getTime()));
                            statisticsDataDAO.setLastDocumentsSentCount(newDocuments.size());

                            callback.onCallback(true);
                        } else {
                            AppLog.w(TAG, "Elasticsearch PUT failed: " + response.message());
                            statisticsDataDAO.increaseTotalFailures();
                            callback.onCallback(false);
                        }
                    });
                }

                @Override
                public void onFailure(@NonNull Call<ElasticResponse> call, @NonNull Throwable t) {
                    Executors.newSingleThreadExecutor().execute(() -> {
                        AppLog.w(TAG, "Elasticsearch PUT failed: " + t.getMessage());
                        statisticsDataDAO.increaseTotalFailures();
                        callback.onCallback(false);
                    });
                }

            });

        } catch (Exception e) {
            AppLog.e(TAG, "Unhandled app error while performing Elasticsearch PUT worker: " + e.getMessage());
            callback.onCallback(false);
            return Result.failure();
        }


        return Result.success();
    }
}
