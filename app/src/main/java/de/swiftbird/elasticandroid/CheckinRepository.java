package de.swiftbird.elasticandroid;

import static androidx.core.content.ContextCompat.getSystemService;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.TextView;


import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import com.google.gson.Gson;

import org.json.JSONObject;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Map;
import java.util.Objects;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

public class CheckinRepository {

    //private final Context context;
    private FleetApi fleetApi;
    private EnrollmentData enrollmentData;
    private static final String TAG = "CheckinRepository";

    // Singleton instance
    private static CheckinRepository instance;
    
    private AlertDialog dialog;

    private TextView tStatus;


    public CheckinRepository(Context context) {
        //this.context = context;
    }

    public static CheckinRepository getInstance(Context context) {
        if (instance == null) {
            instance = new CheckinRepository(context);
        }
        return instance;
    }

    public void checkinAgent(EnrollmentData data, AgentMetadata metadata, StatusCallback callbackActivity, @Nullable AlertDialog dialog, @Nullable TextView tStatus, Context context) {
        this.dialog = dialog;
        this.tStatus = tStatus;
        this.enrollmentData = data;

        Log.i(TAG, "Starting checkin");
        writeDialog("Starting checkin...", true);


        // Initialize Retrofit instance
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(data.fleetUrl)
                .client(NetworkBuilder.getOkHttpClient(data.verifyCert))
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        this.fleetApi = retrofit.create(FleetApi.class);

        String ackToken = null; // TODO: Get ack token from previous checkin response if available

        // Create checkin request
        CheckinRequest checkinRequest = new CheckinRequest("online", ackToken, metadata, "Elastic Agent (Android) checkin.", null, null);
        fleetApi.postCheckin("ApiKey " + data.accessApiKey, data.agentId, checkinRequest).enqueue(new Callback<CheckinResponse>() {
            @Override
            public void onResponse(@NonNull Call<CheckinResponse> call, @NonNull Response<CheckinResponse> response) {
                Log.d(TAG, "Got Response from Fleet Server: " + new Gson().toJson(response.body()));

                if (response.isSuccessful()) {
                    
                    CheckinResponse checkinResponse = response.body();
                    
                    if(checkinResponse == null){
                        Log.e(TAG, "Checkin failed. Received no response from fleet server.");
                        writeDialog("Checkin failed. Received no response from fleet server.", false);
                        callbackActivity.onCallback(false);
                        return;
                    }

                    String actionName = checkinResponse.getActions().get(0).getType();

                    if(Objects.equals(actionName, "REQUEST_DIAGNOSTICS")) {
                        Log.i(TAG, "Received diagnostic request from fleet server.");
                        writeDialog("Received diagnostic request from fleet server.", true);
                        // TODO: Implement diagnostic request handling
                        callbackActivity.onCallback(true);
                        return; // Exit early because we don't have a policy to process

                    } else if (Objects.equals(actionName, "POLICY_CHANGE")) {
                        Log.i(TAG, "Received policy data from fleet server.");
                        writeDialog("Received policy data from fleet server.", true);
                        // Continue with policy data handling below...

                    } else if (Objects.equals(actionName, "UNENROLL")) {
                        Log.i(TAG, "Received unenroll request from fleet server.");
                        writeDialog("Received unenroll request from fleet server.", true);
                        // Delete the enrollment data from the database
                        Log.i(TAG, "Deleting enrollment data from database...");
                        AppDatabase db = AppDatabase.getDatabase(context, "enrollment-data");
                        AppDatabase.databaseWriteExecutor.execute(() -> {
                            db.enrollmentDataDAO().delete();
                            Log.i(TAG, "Enrollment data deleted successfully. Unenrollment complete.");
                            writeDialog("Enrollment data deleted successfully. Unenrollment complete. Please restart the app to re-enroll.", true);
                            callbackActivity.onCallback(true);
                        });

                    } else {
                        Log.e(TAG, "Checkin failed. Received unknown action from fleet server.");
                        writeDialog("Checkin failed. Received unknown action from fleet server.", false);
                        callbackActivity.onCallback(false);
                        return; // Exit early because we don't have a policy to process
                    }


                    try {
                        CheckinResponse.Action.PolicyData.Policy policy = checkinResponse.getActions().get(0).getData().getPolicy();
                    } catch (Exception e) {
                        Log.e(TAG, "Checkin failed. Received POLICY_CHANGE action but policy data could not be parsed. Error: " + e.getMessage());
                        writeDialog("Checkin failed. Received policy change action but policy data could not be parsed from response.", false);
                        callbackActivity.onCallback(false);
                        return;
                    }
                    
                    Log.i(TAG, "Received policy data from fleet server.");
                    writeDialog("Received policy data from fleet server.", true);

                    // Parse policy data
                    PolicyData policyData = parsePolicy(checkinResponse);

                    if(policyData == null){
                        Log.e(TAG, "Checkin failed. Policy data could not be parsed.");
                        writeDialog("Checkin failed. Policy data could not be parsed.", false);
                        callbackActivity.onCallback(false);
                        return;
                    }

                    Log.i(TAG, "Policy data parsed successfully.");
                    writeDialog("Policy data parsed successfully.", true);

                    // Ack the checkin
                    boolean success = ackPolicy(policyData);
                    if(!success){
                        Log.e(TAG, "Checkin failed. Policy data acknowledgement failed.");
                        writeDialog("Checkin failed. Policy data acknowledgement failed.", false);
                        callbackActivity.onCallback(false);
                        return;
                    }

                    AppDatabase.databaseWriteExecutor.execute(() -> {
                        AppDatabase db = AppDatabase.getDatabase(context, "policy-data");
                        PolicyData currentPolicyData = db.policyDataDAO().getPolicyDataSync();

                        if (currentPolicyData != null && currentPolicyData.revision >= policyData.revision) {
                                Log.i(TAG, "Policy data is up to date. No need to update.");

                                // Update lastUpdated to current time format 2024-03-19T21:25:27.937Z API 24
                                @SuppressLint("SimpleDateFormat") SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
                                sdf.setTimeZone(TimeZone.getTimeZone("UTC")); // Ensure the time is in UTC
                                policyData.lastUpdated = sdf.format(Calendar.getInstance().getTime());
                                db.policyDataDAO().refreshPolicyData(policyData.lastUpdated, policyData.actionId);
                                callbackActivity.onCallback(true);

                        } else {
                                Log.i(TAG, "Policy data is outdated. Updating...");
                                db.policyDataDAO().delete(); // Synchronously delete old policy data
                                db.policyDataDAO().insertPolicyData(policyData); // Synchronously insert new policy data
                                Log.i(TAG, "Policy data updated successfully.");

                                // Register background service
                                int interval = policyData.checkinInterval;
                                WorkScheduler.scheduleFleetCheckinWorker(context, interval, TimeUnit.SECONDS);
                                callbackActivity.onCallback(true);
                        }

                    });


                } else {

                    try {
                        try {
                            JSONObject obj = new JSONObject(response.errorBody().string());
                            String message = obj.getString("message");
                            if(message.equals("BadRequest") || message.equals("unauthorized")){
                                writeDialog("The fleet server rejected the enrollment token. Please check the token and try again.", false);
                            } else {
                                writeDialog("Checkin to Fleet failed with code: " + response.code() +  ". Error: " + message, false);
                            }

                        } catch (Exception e) {
                            writeDialog("Checkin to Fleet failed with code: " + response.code() + " (Error message could not be read)", false);
                        }
                        Log.w(TAG, "Checkin to Fleet failed with code: " + response.code() +  ". Context: " + response.toString() + " Response: " + response.errorBody().string());

                    } catch (IOException e) {
                        Log.e(TAG, "Checkin to Fleet failed with code: " + response.code() +  ". Context: " + response.toString() + " (Error body could not be read)");
                    }
                    callbackActivity.onCallback(false);
                }
            }

            @Override
            public void onFailure(@NonNull Call<CheckinResponse> call, Throwable t) {
                if (t instanceof SocketTimeoutException) {
                    Log.i(TAG, "Checkin successful but no new actions were available (timeout).");
                    writeDialog("Checkin successful. No updates.", true);
                    // Update lastUpdated to current time format 2024-03-19T21:25:27.937Z API 24
                    @SuppressLint("SimpleDateFormat") SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
                    sdf.setTimeZone(TimeZone.getTimeZone("UTC")); // Ensure the time is in UTC

                    AppDatabase.databaseWriteExecutor.execute(() -> {
                                AppDatabase db = AppDatabase.getDatabase(context, "policy-data");
                                // Update lastUpdated to current time format 2024-03-19T21:25:27.937Z API 24
                                @SuppressLint("SimpleDateFormat") SimpleDateFormat sdf_2 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
                                sdf_2.setTimeZone(TimeZone.getTimeZone("UTC")); // Ensure the time is in UTC
                                db.policyDataDAO().refreshPolicyData(sdf.format(Calendar.getInstance().getTime()), null);
                            });

                    callbackActivity.onCallback(true);
                } else {
                    // Handle failure
                    writeDialog("Unhandled exception in checkin: " + t.getMessage(), false);
                    Log.e(TAG, "Unhandled exception in checkin: " + t.getMessage());
                    callbackActivity.onCallback(false);
                }

            }
        });

        


    }

    private boolean ackPolicy(PolicyData policyData) {
        Log.i(TAG, "Acknowledging policy data...");
        writeDialog("Acknowledging policy data...", true);

        // Create ack request
        AckRequest ackRequest = new AckRequest("ACTION_RESULT", "ACKNOWLEDGED", enrollmentData.agentId, policyData.actionId, "Policy update success.");
        fleetApi.postAck("ApiKey " + enrollmentData.accessApiKey, enrollmentData.agentId, ackRequest).enqueue(new Callback<AckResponse>() {
            @Override
            public void onResponse(@NonNull Call<AckResponse> call, @NonNull Response<AckResponse> response) {
                Log.d(TAG, "Got Ack Response from Fleet Server: " + new Gson().toJson(response.body()));

                if (response.isSuccessful() && response.body() != null && response.body().getItems() != null && response.body().getItems().length > 0) {
                    if(response.body().getItems()[0].getStatus() == 200) {
                        Log.i(TAG, "Policy data acknowledged successfully.");
                        writeDialog("Policy data acknowledged successfully.", true);
                    } else {
                        writeDialog("Policy data acknowledgement failed with code: " + response.body().getItems()[0].getStatus() +  ". Error: " + response.body().getItems()[0].getMessage(), false);
                        Log.w(TAG, "Policy data acknowledgement failed with code: " + response.body().getItems()[0].getStatus() +  ". Error: " + response.body().getItems()[0].getMessage());
                    }
                } else {
                    try {
                        try {
                            JSONObject obj = new JSONObject(response.errorBody().string());
                            String message = obj.getString("message");
                            writeDialog("Policy data acknowledgement failed with code: " + response.code() +  ". Error: " + message, false);
                        } catch (Exception e) {
                            writeDialog("Policy data acknowledgement failed with code: " + response.code() +  ". (Error message could not be read)", false);
                        }
                        Log.w(TAG, "Policy data acknowledgement failed with code: " + response.code() +  ". Context: " + response.toString() + " Response: " + response.errorBody().string());

                    } catch (Exception e) {
                        Log.e(TAG, "Policy data acknowledgement failed with code: " + response.code() +  ". Context: " + response.toString() + " (Error body could not be read)");
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<AckResponse> call, Throwable t) {
                // Handle failure
                writeDialog("Unhandled exception in policy data acknowledgement: " + t.getMessage(), false);
                Log.e(TAG, "Unhandled exception in policy data acknowledgement: " + t.getMessage());
            }
        });

        return true;
    }

    private void writeDialog(String s, boolean success) {
        if(dialog != null && dialog.isShowing()){
            dialog.setMessage(s);
        }

        if(dialog == null && tStatus != null){
            if(success){
                tStatus.setText("Checkin: " + s);
            } else {
                tStatus.setTextColor(tStatus.getResources().getColor(android.R.color.holo_red_dark));
                tStatus.setText("Checkin Error: " + s);
            }
        }
    }

    private PolicyData parsePolicy(CheckinResponse response) {
        String TAG_PARSE = TAG + " - parsePolicy";

        if (response.getActions() == null || response.getActions().isEmpty()) {
            Log.e(TAG_PARSE, "No actions in response.");
            return null;
        }

        CheckinResponse.Action action = response.getActions().get(0);

        if (action == null || action.getData() == null || action.getData().getPolicy() == null) {
            Log.e(TAG_PARSE, "Action, Action Data, or Policy is null.");
            return null;
        }
        
        CheckinResponse.Action.PolicyData.Policy policy = action.getData().getPolicy();
        PolicyData policyData = new PolicyData();
        policyData.createdAt = action.getCreatedAt();
        policyData.revision = policy.getRevision();

        if (policy.getAgent() == null || policy.getAgent().getProtection() == null) {
            Log.e(TAG_PARSE, "Agent or Protection data is missing.");
            return null;
        }

        policyData.protectionEnabled = policy.getAgent().getProtection().getEnabled();
        policyData.uninstallTokenHash = policy.getAgent().getProtection().getUninstallTokenHash();

        if (policy.getInputs() == null || policy.getInputs().isEmpty() || policy.getInputs().get(0).getStreams() == null || policy.getInputs().get(0).getStreams().isEmpty()) {
            Log.e(TAG_PARSE, "Inputs or Streams data is missing.");
            return null;
        }

        CheckinResponse.Action.Input input = policy.getInputs().get(0);
        policyData.inputName = input.getName();

        CheckinResponse.Action.Input.Stream stream = input.getStreams().get(0);
        policyData.allowUserUnenroll = stream.getAllowUserUnenroll();
        policyData.dataStreamDataset = stream.getDataStream().getDataset();
        policyData.ignoreOlder = stream.getIgnoreOlder();

        // Parse Xm or Xs etc. to seconds
        int checkinIntervalSeconds = timeIntervalToSeconds(stream.getCheckinInterval());
        int putIntervalSeconds = timeIntervalToSeconds(stream.getPutInterval());


        policyData.checkinInterval = checkinIntervalSeconds;
        policyData.putInterval = putIntervalSeconds;

        if(stream.getPaths() == null || stream.getPaths().isEmpty()){
            Log.e(TAG_PARSE, "Path data is missing.");
            return null;
        }

        policyData.paths = String.join(",", stream.getPaths()); // Concatenate paths



        if (policy.getOutputs() == null || policy.getOutputs().isEmpty()) {
            Log.e(TAG_PARSE, "Outputs data is missing.");
            return null;
        }

        Map.Entry<String, CheckinResponse.Action.PolicyData.Policy.Output> entry = policy.getOutputs().entrySet().iterator().next();
        CheckinResponse.Action.PolicyData.Policy.Output output = entry.getValue();

        if(output == null){
            Log.e(TAG_PARSE, "Output data is missing.");
            return null;
        }

        if(output.getApiKey() == null || output.getHosts() == null || output.getSslCaTrustedFingerprint() == null){
            Log.e(TAG_PARSE, "Output data is incomplete.");
            return null;
        }

        policyData.apiKey = output.getApiKey();
        policyData.hosts = String.join(",", output.getHosts()); // Concatenate hosts
        policyData.sslCaTrustedFingerprint = output.getSslCaTrustedFingerprint();

        if(action.getId() == null){
            Log.e(TAG_PARSE, "Action ID is missing.");
            return null;
        }
        policyData.actionId = action.getId();

        // Update lastUpdated to current time format 2024-03-19T21:25:27.937Z API 24
        @SuppressLint("SimpleDateFormat") SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        sdf.setTimeZone(TimeZone.getTimeZone("UTC")); // Ensure the time is in UTC
        policyData.lastUpdated = sdf.format(Calendar.getInstance().getTime());

        return policyData;
    }

    private int timeIntervalToSeconds(String interval){
        // Xm:
        if(interval.contains("m")){
            return Integer.parseInt(interval.replaceAll("[^0-9]", "")) * 60;
        }
        // Xs:
        if(interval.contains("s")){
            return Integer.parseInt(interval.replaceAll("[^0-9]", ""));
        }
        // Xh:
        if(interval.contains("h")){
            return Integer.parseInt(interval.replaceAll("[^0-9]", "")) * 3600;
        }

        Log.w(TAG, "Unknown time interval format: " + interval + ". Defaulting to 60 seconds.");
        return 60;
    }

}



