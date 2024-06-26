package de.swiftbird.elasticandroid;

import android.annotation.SuppressLint;
import android.content.Context;
import android.widget.TextView;
import androidx.annotation.NonNull;
import com.google.gson.Gson;
import org.json.JSONObject;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import java.io.IOException;
import java.util.Objects;

/**
 * This class is responsible for managing enrollment processes, specifically handling the
 * interactions with the Fleet API to enroll devices. It initializes necessary network components
 * and manages UI feedback through TextViews for status and error messages.
 */
public class FleetEnrollRepository {

    // Context from the Android application, used for various operations that require a context.
    private final Context context;

    // Interface for the Fleet API, used to make network requests to the Fleet service.
    private final FleetApi fleetApi;

    // Tag used for logging, helps with categorizing logs from this repository.
    private static final String TAG = "FleetEnrollRepository";

    // TextViews for displaying error messages and status updates in the UI.
    private final TextView tError;
    private final TextView tStatus;

    // Token used for authentication with the Fleet API.
    private final String token;

    // Flag indicating whether SSL certificate verification is enabled for network requests.
    private final boolean verifyCert;

    /**
     * Constructor for FleetEnrollRepository. Initializes the Retrofit client for network
     * operations, sets up the Fleet API interface, and prepares UI components for feedback.
     *
     * @param context     The Android context, used for tasks that require a context.
     * @param serverUrl   The base URL for the Fleet API server.
     * @param token       The authentication token for interacting with the Fleet API.
     * @param certificate The user provided certificate used for SSL verification, if any.
     *                    This is used only if {@code checkCert} is true and the string is not null or empty.
     * @param checkCert   Indicates whether the CA (Certificate Authority) should be checked. If {@code true},
     *                    the method uses the provided {@code certificate} string to pin the certificate
     *                    or the system trust manager if the string is null or empty. If {@code false},
     *                    the client trusts all certificates.
     * @param tStatus     A TextView for displaying status messages to the user.
     * @param tError      A TextView for displaying error messages to the user.
     */
    public FleetEnrollRepository(Context context, String serverUrl, String token, String certificate, boolean checkCert, TextView tStatus, TextView tError) {
        this.context = context;
        this.verifyCert = checkCert;
        this.token = token;
        this.tError = tError;
        this.tStatus = tStatus;

        // Reset current Retrofit instances if they exist
        NetworkBuilder.resetClients();

        // Initialize Retrofit instance for networking
        Retrofit retrofit = NetworkBuilder.getClientFleet(serverUrl, checkCert, certificate, 10);
        fleetApi = retrofit.create(FleetApi.class);
    }

    /**
     * Initiates the enrollment process of an agent with the fleet server. This method starts by logging
     * and displaying the beginning of the enrollment process, followed by making a call to the fleet
     * API to get the fleet server's status. If the fleet server is healthy, it proceeds to send an
     * enrollment request to the server. Depending on the response, it either logs and displays a
     * success message with the new agent ID or handles errors appropriately by logging them and
     * updating the UI with the error message.
     *
     * @param request The {@link AppEnrollRequest} containing details necessary for enrollment, such as the
     *                server URL, hostname, and certificate.
     * @param callbackToEnrollmentActivity A {@link StatusCallback} that allows this method to communicate
     *                                     the outcome of the enrollment process back to the calling activity.
     */
    @SuppressLint("SetTextI18n")
    public void enrollAgent(AppEnrollRequest request, StatusCallback callbackToEnrollmentActivity) {
        AppLog.i(TAG, "Starting enrollment process...");
        tStatus.setText("Starting enrollment process...");
        AppLog.d(TAG, "User provided Server URL: " + request.getServerUrl());
        AppLog.d(TAG, "User provided Hostname: " + request.getHostname());
        AppLog.d(TAG, "User provided certificate: " + request.getCertificate());

        // First call API info endpoint:
        AppLog.i(TAG, "Requesting Fleet Server details...");
        tStatus.setText("Requesting Fleet Server details...");
        fleetApi.getFleetStatus().enqueue(new Callback<FleetStatusResponse>() {
            @Override
            public void onResponse(@NonNull Call<FleetStatusResponse> call, @NonNull Response<FleetStatusResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    AppLog.i(TAG, "Fleet Name: " + response.body().getName());
                    AppLog.i(TAG, "Fleet Status: " + response.body().getStatus());

                    if (response.body().getStatus().equals("HEALTHY")) {
                        AppLog.i(TAG, "Fleet is HEALTHY. Sending enrollment request to fleet server...");
                        tStatus.setText("Fleet server is healthy. Sending enrollment request to fleet server (this may take a while)...");

                        FleetEnrollRequest enrollRequest = new FleetEnrollRequest();
                        enrollRequest.setType("PERMANENT");
                        AgentMetadata metadata = AgentMetadata.getMetadataFromDeviceAndDB(null, request.getHostname());
                        enrollRequest.setMetadata(metadata);

                        fleetApi.enrollAgent("ApiKey " + token, enrollRequest).enqueue(new Callback<>() {
                            @Override
                            public void onResponse(@NonNull Call<FleetEnrollResponse> call, @NonNull Response<FleetEnrollResponse> response) {
                                if (response.isSuccessful()) {
                                    FleetEnrollResponse enrollResponse = response.body();

                                    if(enrollResponse != null && enrollResponse.getItem() != null && enrollResponse.getItem().getId() != null){
                                        AppLog.i(TAG, "Enrollment successful. Agent ID: " + enrollResponse.getItem().getId());
                                        tStatus.setText("Enrollment successful. Agent ID: " + enrollResponse.getItem().getId());

                                        AppLog.d(TAG, "Response from Fleet Server: " + new Gson().toJson(enrollResponse));

                                        // Save the enrollment info and insert initial statistic to the database
                                        FleetEnrollData enrollmentData = parseAndSaveEnrollmentInfo(request, enrollResponse);
                                        
                                        AppLog.i(TAG, "Enrollment data saved to database. Starting initial checkin...");
                                        // We are not done yet. We need to do the initial checkin to get the policy
                                        FleetCheckinRepository checkinRepository = new FleetCheckinRepository(null, tStatus);
                                        checkinRepository.checkinAgent(context, enrollmentData, metadata, callbackToEnrollmentActivity);
                                    } else {
                                        tError.setText("Enrollment failed because of invalid response from Fleet Server.");
                                        AppLog.e(TAG, "Enrollment failed because of invalid response from Fleet Server. Context: " + response);
                                        callbackToEnrollmentActivity.onCallback(false);
                                    }
                                } else {

                                    try {
                                        try {
                                            JSONObject obj = new JSONObject(Objects.requireNonNull(response.errorBody()).string());
                                            String message = obj.getString("message");
                                            if(message.equals("BadRequest") || message.equals("unauthorized")){
                                                tError.setText("The fleet server rejected the enrollment token. Please check the token and try again.");
                                            } else {
                                                tError.setText("Enrollment failed with code: " + response.code() +  ". Error: " + message);
                                            }

                                        } catch (Exception e) {
                                            tError.setText("Enrollment failed with code: " + response.code() + " (Error message could not be read)");
                                        }
                                        AppLog.w(TAG, "Enrollment failed with code: " + response.code() +  ". Context: " + response + " Response: " + Objects.requireNonNull(response.errorBody()).string());

                                    } catch (IOException e) {
                                        AppLog.e(TAG, "Enrollment failed with code: " + response.code() +  ". Context: " + response + " (Error body could not be read)");
                                    }
                                    callbackToEnrollmentActivity.onCallback(false);
                                }
                            }

                            @Override
                            public void onFailure(@NonNull Call<FleetEnrollResponse> call, @NonNull Throwable t) {
                                tError.setText("Enrollment failed with error: " + t.getMessage());
                                AppLog.e(TAG, "Enrollment failed with error: " + t.getMessage());
                                callbackToEnrollmentActivity.onCallback(false);
                            }
                        });


                    } else {
                        tError.setText("Fleet server is not healthy - Status: " + response.body().getStatus());
                        AppLog.w(TAG, "Fleet Server is not healthy. Stopping enrollment.");
                        callbackToEnrollmentActivity.onCallback(false);
                    }
                } else {
                    tError.setText("Could not communicate with fleet server - Check server URL and try again. Status Code: " + response.code());
                    AppLog.e(TAG, "Unhandled externally caused exception in initial Fleet Server request. Stopping enrollment. Response: " + response);
                    callbackToEnrollmentActivity.onCallback(false);
                }
            }

            @Override
            public void onFailure(@NonNull Call<FleetStatusResponse> call, @NonNull Throwable t) {
                // Check if its an SSL error
                if(Objects.requireNonNull(t.getMessage()).contains("CertPathValidatorException")){
                    tError.setText("Could not communicate with Fleet Server - SSL/TLS Handshake failed. Please check the certificate (or disable certificate verification) and try again.");
                    AppLog.e(TAG, "SSL/TLS Handshake failed. Stopping enrollment - Error: " + t);
                } else {
                    tError.setText("Could not communicate with Fleet Server - Error: " + t.getMessage());
                    AppLog.e(TAG, "Unhandled exception in initial Fleet Server request. Stopping enrollment - Error: " + t);
                }
                callbackToEnrollmentActivity.onCallback(false);
            }
        });


    }

    /**
     * Parses the enrollment response from the Fleet server and saves the enrollment data to the local database.
     *
     * @param request The {@link AppEnrollRequest} containing details necessary for enrollment.
     * @param response The {@link FleetEnrollResponse} containing the enrollment response from the Fleet server.
     * @return The {@link FleetEnrollData} object representing the enrollment data.
     */
    private FleetEnrollData parseAndSaveEnrollmentInfo(AppEnrollRequest request, FleetEnrollResponse response) {
        FleetEnrollResponse.Item item = response.getItem();
        FleetEnrollData enrollmentData = new FleetEnrollData();
        enrollmentData.id = 1; // We only have one enrollment info in the database
        enrollmentData.action = response.getAction();

        // Save the Fleet API key only encrypted in the secure preferences
        AppSecurePreferences securePreferences = AppSecurePreferences.getInstance(context);
        securePreferences.saveFleetApiKey(item.getAccessApiKey());

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
        enrollmentData.fleetCertificate = request.getCertificate();

        new Thread(() -> {
            AppDatabase db = AppDatabase.getDatabase(context);
            db.enrollmentDataDAO().insertEnrollmentInfo(enrollmentData);
            AppLog.i(TAG, "Saving Enrollment data to db successful.");
            // Saving initial statistics data
            db.statisticsDataDAO().insert(new AppStatisticsData());
        }).start();

        return enrollmentData;
    }
}
