package de.swiftbird.elasticandroid;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.Log;
import android.util.TypedValue;
import android.widget.TextView;


import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.gson.Gson;

import org.json.JSONObject;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import java.io.IOException;
import java.util.UUID;

public class EnrollmentRepository {

    private final Context context;
    private FleetApi fleetApi;
    private static final String TAG = "EnrollmentRepository";
    private TextView tError, tStatus;
    private String token;

    private boolean verifyCert;


    public EnrollmentRepository(Context context, String serverUrl, String token, boolean checkCert, TextView tStatus, TextView tError) {
        this.context = context;
        this.verifyCert = checkCert;
        // Initialize Retrofit instance
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(serverUrl)
                .client(NetworkBuilder.getOkHttpClient(checkCert))
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        fleetApi = retrofit.create(FleetApi.class);
        this.token = token;

        this.tError = tError;
        this.tStatus = tStatus;
    }

    public void enrollAgent(AppEnrollRequest request, de.swiftbird.elasticandroid.Callback callbackToEnrollmentActivity) {
        Log.i(TAG, "Starting enrollment process...");
        tStatus.setText("Starting enrollment process...");
        Log.d(TAG, "User provided Server URL: " + request.getServerUrl());
        Log.d(TAG, "User provided Hostname: " + request.getHostname());
        Log.d(TAG, "User provided certificate: " + request.getCertificate());

        // First call API info endpoint:
        Log.i(TAG, "Requesting Fleet Server details...");
        tStatus.setText("Requesting Fleet Server details...");

        fleetApi.getFleetStatus().enqueue(new Callback<FleetStatusResponse>() {

            @Override
            public void onResponse(Call<FleetStatusResponse> call, Response<FleetStatusResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Log.i(TAG, "Fleet Name: " + response.body().getName());
                    Log.i(TAG, "Fleet Status: " + response.body().getStatus());

                    if (response.body().getStatus().equals("HEALTHY")) {
                        Log.i(TAG, "Fleet is HEALTHY. Sending enrollment request to fleet server...");
                        tStatus.setText("Fleet server is healthy. Sending enrollment request to fleet server (this may take a while)...");


                        FleetEnrollRequest enrollRequest = new FleetEnrollRequest();
                        enrollRequest.setType("PERMANENT");
                        String randomAgentId = UUID.randomUUID().toString();
                        AgentMetadata metadata = AgentMetadata.getMetadataFromDeviceAndDB(null, request.getHostname());
                        enrollRequest.setMetadata(metadata);

                        fleetApi.enrollAgent("ApiKey " + token, enrollRequest).enqueue(new Callback<FleetEnrollResponse>() {
                            @Override
                            public void onResponse(@NonNull Call<FleetEnrollResponse> call, @NonNull Response<FleetEnrollResponse> response) {
                                if (response.isSuccessful()) {
                                    FleetEnrollResponse enrollResponse = response.body();

                                    if(enrollResponse != null && enrollResponse.getItem() != null && enrollResponse.getItem().getId() != null){
                                        Log.i(TAG, "Enrollment successful. Agent ID: " + enrollResponse.getItem().getId());
                                        tStatus.setText("Enrollment successful. Agent ID: " + enrollResponse.getItem().getId());

                                        Log.d(TAG, "Response from Fleet Server: " + new Gson().toJson(enrollResponse));

                                        // Save the enrollment info to the database
                                        EnrollmentData enrollmentData = parseAndSaveEnrollmentInfo(request, enrollResponse);

                                        Log.i(TAG, "Enrollment data saved to database. Starting initial checkin...");
                                        // We are not done yet. We need to do the initial checkin to get the policy
                                        CheckinRepository checkinRepository = CheckinRepository.getInstance(context);

                                        Context context2 = new androidx.appcompat.view.ContextThemeWrapper(context, R.style.Theme_ElasticAgentAndroid);

                                        try {
                                            TypedValue typedValue = new TypedValue();
                                            context2.getTheme().resolveAttribute(R.color.elastic_agent_green, typedValue, true);
                                            Log.d("ThemeCheck", "colorPrimary: " + typedValue.resourceId + ", expected: " + R.color.elastic_agent_green);
                                        } catch (Exception e) {
                                            Log.e("ThemeCheck", "Error checking theme", e);
                                        }


                                        checkinRepository.checkinAgent(enrollmentData, metadata, callbackToEnrollmentActivity, null, tStatus, context);


                                    } else {
                                        tError.setText("Enrollment failed because of invalid response from Fleet Server.");
                                        Log.e(TAG, "Enrollment failed because of invalid response from Fleet Server. Context: " + response.toString());
                                        callbackToEnrollmentActivity.onCallback(false);
                                    }
                                } else {

                                    try {
                                        try {
                                            JSONObject obj = new JSONObject(response.errorBody().string());
                                            String message = obj.getString("message");
                                            if(message.equals("BadRequest") || message.equals("unauthorized")){
                                                tError.setText("The fleet server rejected the enrollment token. Please check the token and try again.");
                                            } else {
                                                tError.setText("Enrollment failed with code: " + response.code() +  ". Error: " + message);
                                            }

                                        } catch (Exception e) {
                                            tError.setText("Enrollment failed with code: " + response.code() + " (Error message could not be read)");
                                        }
                                        Log.w(TAG, "Enrollment failed with code: " + response.code() +  ". Context: " + response.toString() + " Response: " + response.errorBody().string());

                                    } catch (IOException e) {
                                        Log.e(TAG, "Enrollment failed with code: " + response.code() +  ". Context: " + response.toString() + " (Error body could not be read)");
                                    }
                                    callbackToEnrollmentActivity.onCallback(false);
                                }
                            }

                            @Override
                            public void onFailure(Call<FleetEnrollResponse> call, Throwable t) {
                                tError.setText("Enrollment failed with error: " + t.getMessage());
                                Log.e(TAG, "Enrollment failed with error: " + t.getMessage());
                                callbackToEnrollmentActivity.onCallback(false);
                            }
                        });


                    } else {
                        tError.setText("Fleet server is not healthy - Status: " + response.body().getStatus());
                        Log.w(TAG, "Fleet Server is not healthy. Stopping enrollment.");
                        callbackToEnrollmentActivity.onCallback(false);
                    }
                } else {
                    tError.setText("Could not communicate with fleet server - Check server URL and try again. Status Code: " + response.code());
                    Log.e(TAG, "Unhandled externally caused exception in initial Fleet Server request. Stopping enrollment. Response: " + response.toString());
                    callbackToEnrollmentActivity.onCallback(false);
                }
            }

            @Override
            public void onFailure(Call<FleetStatusResponse> call, Throwable t) {
                // Handle failure
                tError.setText("Could not communicate with Fleet Server - Error: " + t.getMessage());
                Log.e(TAG, "Unhandled exception in initial Fleet Server request. Stopping enrollment. Response: " + call.toString() + " Error: " + t.toString());
                callbackToEnrollmentActivity.onCallback(false);
            }
        });


    }

    private EnrollmentData parseAndSaveEnrollmentInfo(AppEnrollRequest request, FleetEnrollResponse response) {

        FleetEnrollResponse.Item item = response.getItem();

        EnrollmentData enrollmentData = new EnrollmentData();

        enrollmentData.id = 1; // We only have one enrollment info in the database
        enrollmentData.action = response.getAction();
        enrollmentData.accessApiKey = item.getAccessApiKey();
        enrollmentData.accessApiKeyId = item.getAccessApiKeyId();
        enrollmentData.active = item.getActive();
        enrollmentData.enrolledAt = item.getEnrolledAt();
        enrollmentData.policyId = item.getPolicyId();
        enrollmentData.status = item.getStatus();
        enrollmentData.isEnrolled = true;
        enrollmentData.hostname = request.getHostname();
        enrollmentData.agentId = item.getId();
        enrollmentData.fleetUrl = request.getServerUrl();
        enrollmentData.verifyCert = verifyCert;

        new Thread(new Runnable() {
            @Override
            public void run() {
                AppDatabase db = AppDatabase.getDatabase(context, "enrollment-data");
                db.enrollmentDataDAO().insertEnrollmentInfo(enrollmentData);
                Log.i(TAG, "Saving Enrollment data to db successful.");
            }
        }).start();

        return enrollmentData;
    }





}
