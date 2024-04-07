package de.swiftbird.elasticandroid;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.ListenableWorker;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.google.gson.Gson;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Objects;
import java.util.TimeZone;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Background worker responsible for transmitting buffered documents from all active components
 * to Elasticsearch using bulk operations. It fetches enrollment and policy data to configure
 * the request appropriately, also handling exponential backoff strategies based on policy configurations
 * and request outcomes.
 */
public class ElasticWorker extends Worker {

    private static final String TAG = "ElasticWorker"; //TODO: Make this static final for every class

    public ElasticWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
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
        AppLog.i("FleetCheckinWorker", "Performing elasticsearch PUT from background worker");

        // Obtain an instance of the AppDatabase
        AppDatabase db = AppDatabase.getDatabase(this.getApplicationContext(), "enrollment-data");

        // Synchronously fetch the enrollment data; adjust the method call as necessary based on your DAO
        FleetEnrollData enrollmentData = db.enrollmentDataDAO().getEnrollmentInfoSync(1);
        AgentMetadata agentMetadata = AgentMetadata.getMetadataFromDeviceAndDB(enrollmentData.agentId, enrollmentData.hostname);
        PolicyData policyData = db.policyDataDAO().getPolicyDataSync();
        AppStatisticsData statisticsData = db.statisticsDataDAO().getStatisticsSync();
        StatusCallback callback = new StatusCallback() {

            /**
             * Callback method to handle the result of the Elasticsearch PUT operation.
             *
             * @param success True if the operation was successful, false otherwise.
             */
            @Override
            public void onCallback(boolean success) {
                // Create new thread to handle the callback
                Executors.newSingleThreadExecutor().execute(() -> {
                    if (!success) {
                        AppStatisticsDataDAO statisticsData = db.statisticsDataDAO();
                        statisticsData.increaseTotalFailures();

                        // Use exponential backoff for the next check-in if enanbled
                        if (policyData.useBackoff) {
                            int intendedBackoff = policyData.backoffPutInterval * 2;

                            // Use old backoff interval if the current interval is greater than the max interval
                            if (intendedBackoff > policyData.maxBackoffInterval) {
                                policyData.backoffPutInterval = policyData.maxBackoffInterval;
                                db.policyDataDAO().setBackoffPutInterval(policyData.maxBackoffInterval);
                            } else {
                                // Configure the new backoff interval
                                policyData.backoffPutInterval = intendedBackoff;
                                db.policyDataDAO().setBackoffPutInterval(intendedBackoff);
                            }

                            // Set agent health status to unhealthy
                            db.statisticsDataDAO().setAgentHealth("Unhealthy");
                            AppLog.i("ElasticWorker", "Elasticsearch PUT failed (or empty and 'backoff if empty' enabled), increasing interval to " + policyData.backoffPutInterval + " seconds");
                        }

                    } else {
                        // If Fleet check-in also succeeded, reset the agent health status
                        if (policyData.backoffCheckinInterval == policyData.checkinInterval) {
                            db.statisticsDataDAO().setAgentHealth("Healthy");
                        }
                        // Reset the backoff interval
                        policyData.backoffPutInterval = policyData.putInterval;
                        db.policyDataDAO().resetBackoffPutInterval();
                    }

                    // Schedule the next check-in
                    AppLog.i("ElasticWorker", "Scheduling next Elasticsearch PUT in " + policyData.backoffPutInterval + " seconds");
                    WorkScheduler.scheduleElasticsearchWorker(getApplicationContext(), policyData.backoffPutInterval, TimeUnit.SECONDS);
                });
            }
        };

        // Perform the Elasticsearch PUT operation
        return getDocumentsFromComponents(db, enrollmentData, policyData, callback);
    }

    /**
     * The main logic of the worker. It iterates over all components specified in the policy data,
     * initializes them, and collects documents from each component's buffer.
     */
    private ListenableWorker.Result getDocumentsFromComponents(AppDatabase db, FleetEnrollData enrollmentData, PolicyData policyData, StatusCallback callback) {
        try {
            ArrayList<ElasticDocument> newDocuments = new ArrayList<>();

            for (String componentPath : policyData.paths.split(",")) {
                try {
                    // Remove everything behind the first "." to get the component name
                    String componentName = componentPath.split("\\.")[0];

                    String subComponent = "";
                    if (componentPath.split("\\.").length >= 2) {
                        subComponent = componentPath.split("\\.")[1];
                    }

                    Component component = ComponentFactory.createInstance(componentName);
                    if (!component.setup(getApplicationContext(), enrollmentData, policyData, subComponent)) {
                        AppLog.w(TAG, "Component " + component.getPathName() + " setup failed");
                        continue;
                    }


                    List<ElasticDocument> bufferedDocuments = component.getDocumentsFromBuffer(policyData.maxDocumentsPerRequest);

                    if (bufferedDocuments == null) {
                        AppLog.w(TAG, "Component " + component.getPathName() + " returned null documents");
                        continue;
                    }
                    newDocuments.addAll(bufferedDocuments);

                } catch (Exception e) {
                    if (e instanceof IllegalArgumentException && Objects.requireNonNull(e.getMessage()).contains("not found")) {
                        AppLog.w(TAG, "Component path " + componentPath + " defined in policy but app does not support it");
                    } else {
                        AppLog.e(TAG, "Unhandled app error while processing component: " + Arrays.toString(e.getStackTrace()));
                    }
                }
            }

            // Now disable any components that were _not_ in the paths list
            for (Component component : ComponentFactory.getAllInstances()) {
                if (!policyData.paths.contains(component.getPathName())) {
                    AppLog.i(TAG, "Disabling component: " + component.getPathName());
                    component.disable(getApplicationContext(), enrollmentData, policyData);
                }
            }

            // Check if newDocuments is empty
            if (newDocuments.isEmpty()) {
                AppLog.i(TAG, "No documents to send to Elasticsearch");
                if (policyData.backoffOnEmptyBuffer) {
                    AppLog.d(TAG, "Backoff on empty buffer enabled, will increase next backoff interval if possible");
                    callback.onCallback(false);
                } else {
                    AppLog.d(TAG, "Backoff on empty buffer disabled, will not increase next backoff interval");
                    callback.onCallback(true);
                }
                return Result.success();
            }

            // Send the documents to Elasticsearch
            return sendBulkToElasticsearch(db, policyData, newDocuments, callback);


        } catch (Exception e) {
            AppLog.e(TAG, "Unhandled app error while performing Elasticsearch PUT worker: " + e.getMessage());
            callback.onCallback(false);
            return Result.failure();
        }
    }

    /**
     * Sends a bulk request to Elasticsearch with the provided documents.
     *
     * @param db           The AppDatabase instance.
     * @param policyData   The policy data for the current agent.
     * @param newDocuments The list of documents to send to Elasticsearch.
     * @param callback     The callback to handle the result of the operation.
     * @return The result of the operation.
     */
    private ListenableWorker.Result sendBulkToElasticsearch(AppDatabase db, PolicyData policyData, List<ElasticDocument> newDocuments, StatusCallback callback) {
        AppStatisticsDataDAO statisticsDataDAO = db.statisticsDataDAO();
        statisticsDataDAO.decreaseCombinedBufferSize(newDocuments.size());

        // Make a PUT request to Elasticsearch using Retrofit
        String sslFingerprint = policyData.sslCaTrustedFingerprint;
        String sslFullCert = policyData.sslCaTrustedFull;
        String esUrl = policyData.hosts;
        boolean verifyCert = true; // we use the full cert provided by fleet anyway
        String elasticAccessApiKey = policyData.apiKey;
        int timeoutSeconds = 30;

        // Base64 encode the API key
        String elasticAccessApiKeyEncoded = android.util.Base64.encodeToString(elasticAccessApiKey.getBytes(), android.util.Base64.NO_WRAP);

        String indexName = policyData.dataStreamDataset;

        // Prepend logs- to the index name
        if (!indexName.startsWith("logs-")) {
            indexName = "logs-" + indexName + "-2.3.0"; // TODO: Make this dynamic
        }

        Gson gson = new Gson();
        RequestBody requestBody = createBulkRequestBody(newDocuments, gson);

        // Initialize Retrofit instance
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(esUrl)
                .client(NetworkBuilder.getOkHttpClient(verifyCert, sslFullCert, timeoutSeconds))
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        ElasticApi elasticApi = retrofit.create(ElasticApi.class);
        elasticApi.putBulk("ApiKey " + elasticAccessApiKeyEncoded, indexName, requestBody).enqueue(new Callback<ElasticResponse>() {
            @Override
            public void onResponse(@NonNull Call<ElasticResponse> call, @NonNull Response<ElasticResponse> response) {
                AppLog.d(TAG, "Got Response from Elasticsearch Server: " + new Gson().toJson(response.body()));

                Executors.newSingleThreadExecutor().execute(() -> {
                    if (response.isSuccessful()) {
                        if (response.body() == null || response.body().getErrors().equals("true")) {
                            AppLog.w(TAG, "Elasticsearch PUT failed: " + response.message());
                            statisticsDataDAO.increaseTotalFailures();
                            callback.onCallback(false);
                            return;
                        }

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

        return Result.success();
    }


    /**
     * Creates the body for a bulk request to Elasticsearch. Each document is preprocessed
     * with necessary action metadata before serialization.
     *
     * @param documents The list of documents to be sent in the bulk operation.
     * @param gson The Gson instance for serialization.
     * @return The RequestBody suitable for the bulk API request.
     */
    private RequestBody createBulkRequestBody(List<ElasticDocument> documents, Gson gson) {
        StringBuilder bulkPayload = new StringBuilder();

        for (ElasticDocument document : documents) {
            // Assuming you have an action metadata line for each document (e.g., index action)
            String actionMetadata = createActionMetadata(document);
            String documentJson = gson.toJson(document);

            bulkPayload.append(actionMetadata).append("\n");
            bulkPayload.append(documentJson).append("\n");
        }
        Log.d(TAG, "Bulk payload: " + bulkPayload.toString());
        return RequestBody.create(MediaType.parse("application/x-ndjson"), bulkPayload.toString());
    }


    /**
     * Generates action metadata for a document to be included in a bulk operation.
     * This metadata instructs Elasticsearch on how to process each document.
     *
     * @param document The document for which to create action metadata.
     * @return A JSON string representing the action metadata for the document.
     */
    private String createActionMetadata(ElasticDocument document) {
        return "{\"create\": {}}";
    }


}
