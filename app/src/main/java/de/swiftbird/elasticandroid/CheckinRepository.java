package de.swiftbird.elasticandroid;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;


import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.lifecycle.LifecycleOwner;
import androidx.room.Room;

import com.google.gson.Gson;

import org.json.JSONObject;

import okhttp3.Authenticator;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Route;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import java.io.IOException;
import java.security.cert.CertificateException;
import java.text.SimpleDateFormat;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Map;
import java.util.TimeZone;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import okhttp3.OkHttpClient;

public class CheckinRepository {

    //private final Context context;
    private FleetApi fleetApi;
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

    public void checkinAgent(EnrollmentData data, AgentMetadata metadata,  de.swiftbird.elasticandroid.Callback callbackActivity, @Nullable AlertDialog dialog, @Nullable TextView tStatus,  Context context) {
        this.dialog = dialog;
        this.tStatus = tStatus;

        Log.i(TAG, "Starting checkin");
        writeDialog("Starting checkin...", true);


        // Initialize Retrofit instance
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(data.fleetUrl)
                .client(NetworkBuilder.getOkHttpClient(data.verifyCert))
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        fleetApi = retrofit.create(FleetApi.class);

        // Create checkin request
        CheckinRequest checkinRequest = new CheckinRequest("CHECKIN", null, metadata, "Elastic Agent (Android) checkin.", null, null);
        fleetApi.postCheckin("ApiKey " + data.accessApiKey, data.agentId, checkinRequest).enqueue(new Callback<CheckinResponse>() {
            @Override
            public void onResponse(@NonNull Call<CheckinResponse> call, @NonNull Response<CheckinResponse> response) {
                if (response.isSuccessful()) {
                    
                    CheckinResponse checkinResponse = response.body();
                    
                    if(checkinResponse == null){
                        Log.e(TAG, "Checkin failed. Received no response from fleet server.");
                        writeDialog("Checkin failed. Received no response from fleet server.", false);
                        callbackActivity.onCallback(false);
                        return;
                    }
                    
                    CheckinResponse.Action.PolicyData.Policy policy = checkinResponse.getActions().get(0).getData().getPolicy();
                    
                    if(policy == null) {
                        Log.e(TAG, "Checkin failed. Received no policy data from fleet server.");
                        writeDialog("Checkin failed. Received no policy data from fleet server.", false);
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

                    AppDatabase.databaseWriteExecutor.execute(() -> {
                        AppDatabase db = AppDatabase.getDatabase(context, "policy-data");
                        PolicyData currentPolicyData = db.policyDataDAO().getPolicyDataSync();

                        if (currentPolicyData != null && currentPolicyData.revision >= policyData.revision) {
                                Log.i(TAG, "Policy data is up to date. No need to update.");

                                // Update lastUpdated to current time format 2024-03-19T21:25:27.937Z API 24
                                @SuppressLint("SimpleDateFormat") SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
                                sdf.setTimeZone(TimeZone.getTimeZone("UTC")); // Ensure the time is in UTC
                                policyData.lastUpdated = sdf.format(Calendar.getInstance().getTime());
                                db.policyDataDAO().updateLastUpdated(policyData.lastUpdated);

                                callbackActivity.onCallback(true);
                        } else {
                                Log.i(TAG, "Policy data is outdated. Updating...");
                                db.policyDataDAO().delete(); // Synchronously delete old policy data
                                db.policyDataDAO().insertPolicyData(policyData); // Synchronously insert new policy data
                                Log.i(TAG, "Policy data updated successfully.");
                                //TODO: Send acknowledgement to fleet server
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
                // Handle failure
                writeDialog("Unhandled exception in checkin: " + t.getMessage(), false);
                Log.e(TAG, "Unhandled exception in checkin: " + t.getMessage());
                callbackActivity.onCallback(false);

            }
        });

        


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

        CheckinResponse.Action action = response.getActions().get(0); // Assuming the first action is of interest

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
        policyData.interval = stream.getInterval();

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

        // Update lastUpdated to current time format 2024-03-19T21:25:27.937Z API 24
        @SuppressLint("SimpleDateFormat") SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        sdf.setTimeZone(TimeZone.getTimeZone("UTC")); // Ensure the time is in UTC
        policyData.lastUpdated = sdf.format(Calendar.getInstance().getTime());

        return policyData;
    }



}



