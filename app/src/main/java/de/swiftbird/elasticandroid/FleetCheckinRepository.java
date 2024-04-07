package de.swiftbird.elasticandroid;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
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
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

/**
 * Manages check-in processes between the Elastic Agent and Fleet server, including
 * sending check-in requests, processing Fleet server responses, and updating local
 * policy data based on the server's instructions. This class uses Retrofit to
 * communicate with the Fleet server and handles the parsing of policy data received.
 */
public class FleetCheckinRepository {

    //private final Context context;
    private FleetApi fleetApi;
    private FleetEnrollData enrollmentData;
    private static final String TAG = "FleetCheckinRepository";

    // Singleton instance
    private static FleetCheckinRepository instance;
    
    private AlertDialog dialog;

    private TextView tStatus;


    /**
     * Constructor for the repository. Initializes Retrofit and other necessary components.
     *
     * @param context Application context for initializing network components.
     */
    public FleetCheckinRepository(Context context) {
        //this.context = context;
    }

    /**
     * Gets the singleton instance of the FleetCheckinRepository, creating it if necessary.
     *
     * @param context Application context for the repository.
     * @return The singleton instance of the FleetCheckinRepository.
     */
    public static FleetCheckinRepository getInstance(Context context) {
        if (instance == null) {
            instance = new FleetCheckinRepository(context);
        }
        return instance;
    }

    /**
     * Performs a check-in operation with the Fleet server, updating the agent's state
     * and policies based on the response.
     *
     * @param data The current enrollment data of the agent.
     * @param metadata Metadata about the agent to be included in the check-in request.
     * @param callbackActivity Callback to handle UI updates based on the check-in result.
     * @param dialog Optional dialog for displaying status messages (used when updating policy via the MainActivity button).
     * @param tStatus Optional TextView for displaying status messages (used to display check-in status in the EnrollActivity during enrollment).
     * @param context Application context.
     */
    public void checkinAgent(FleetEnrollData data, AgentMetadata metadata, StatusCallback callbackActivity, @Nullable AlertDialog dialog, @Nullable TextView tStatus, Context context) {
        this.dialog = dialog;
        this.tStatus = tStatus;
        this.enrollmentData = data;

        AppLog.i(TAG, "Starting checkin");
        writeDialog("Starting checkin...", true);


        // Initialize Retrofit instance
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(data.fleetUrl)
                .client(NetworkBuilder.getOkHttpClient(data.verifyCert, null, 5))
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        this.fleetApi = retrofit.create(FleetApi.class);

        String ackToken = null; // TODO: Get ack token from previous checkin response if available

        // Create checkin request
        FleetCheckinRequest checkinRequest = new FleetCheckinRequest("online", ackToken, metadata, "Elastic Agent (Android) checkin.", null, null);
        fleetApi.postCheckin("ApiKey " + data.accessApiKey, data.agentId, checkinRequest).enqueue(new Callback<FleetCheckinResponse>() {
            @Override
            public void onResponse(@NonNull Call<FleetCheckinResponse> call, @NonNull Response<FleetCheckinResponse> response) {
                AppLog.d(TAG, "Got Response from Fleet Server: " + new Gson().toJson(response.body()));

                if (response.isSuccessful()) {
                    
                    FleetCheckinResponse checkinResponse = response.body();
                    
                    if(checkinResponse == null){
                        AppLog.e(TAG, "Checkin failed. Received no response from fleet server.");
                        writeDialog("Checkin failed. Received no response from fleet server.", false);
                        callbackActivity.onCallback(false);
                        return;
                    }

                    String actionName = checkinResponse.getActions().get(0).getType();

                    if(Objects.equals(actionName, "REQUEST_DIAGNOSTICS")) {
                        AppLog.i(TAG, "Received diagnostic request from fleet server.");
                        writeDialog("Received diagnostic request from fleet server.", true);
                        // TODO: Implement diagnostic request handling
                        callbackActivity.onCallback(true);
                        return; // Exit early because we don't have a policy to process

                    } else if (Objects.equals(actionName, "POLICY_CHANGE")) {
                        AppLog.i(TAG, "Received policy data from fleet server.");
                        writeDialog("Received policy data from fleet server.", true);
                        // Continue with policy data handling below...

                    } else if (Objects.equals(actionName, "UNENROLL")) {
                        AppLog.i(TAG, "Received unenroll request from fleet server.");
                        writeDialog("Received unenroll request from fleet server.", true);
                        // Cancel all scheduled work
                        WorkScheduler.cancelAllWork(context);
                        // Delete the enrollment data from the database
                        AppLog.i(TAG, "Deleting enrollment data from database...");
                        AppDatabase db = AppDatabase.getDatabase(context, "enrollment-data");
                        AppDatabase.databaseWriteExecutor.execute(() -> {
                            db.enrollmentDataDAO().delete();
                            db.policyDataDAO().delete();
                            db.statisticsDataDAO().delete();
                            AppLog.i(TAG, "Enrollment data deleted successfully. Unenrollment complete.");
                            writeDialog("Enrollment data deleted successfully. Unenrollment complete. Please restart the app to re-enroll.", true);
                            callbackActivity.onCallback(true);
                        });

                    } else {
                        AppLog.e(TAG, "Checkin failed. Received unknown action from fleet server.");
                        writeDialog("Checkin failed. Received unknown action from fleet server.", false);
                        callbackActivity.onCallback(false);
                        return; // Exit early because we don't have a policy to process
                    }


                    try {
                        FleetCheckinResponse.Action.PolicyData.Policy policy = checkinResponse.getActions().get(0).getData().getPolicy();
                    } catch (Exception e) {
                        AppLog.e(TAG, "Checkin failed. Received POLICY_CHANGE action but policy data could not be parsed. Error: " + e.getMessage());
                        writeDialog("Checkin failed. Received policy change action but policy data could not be parsed from response.", false);
                        callbackActivity.onCallback(false);
                        return;
                    }
                    
                    AppLog.i(TAG, "Received policy data from fleet server.");
                    writeDialog("Received policy data from fleet server.", true);

                    // Parse policy data
                    PolicyData policyData = parsePolicy(checkinResponse);

                    if(policyData == null){
                        AppLog.e(TAG, "Checkin failed. Policy data could not be parsed.");
                        writeDialog("Checkin failed. Policy data could not be parsed.", false);
                        callbackActivity.onCallback(false);
                        return;
                    }

                    AppLog.i(TAG, "Policy data parsed successfully.");
                    writeDialog("Policy data parsed successfully.", true);

                    // Ack the checkin
                    boolean success = ackPolicy(policyData);
                    if(!success){
                        AppLog.e(TAG, "Checkin failed. Policy data acknowledgement failed.");
                        writeDialog("Checkin failed. Policy data acknowledgement failed.", false);
                        callbackActivity.onCallback(false);
                        return;
                    }

                    AppDatabase.databaseWriteExecutor.execute(() -> {
                        AppDatabase db = AppDatabase.getDatabase(context, "policy-data");
                        PolicyData currentPolicyData = db.policyDataDAO().getPolicyDataSync();

                        if (currentPolicyData != null && currentPolicyData.revision >= policyData.revision) {
                                AppLog.i(TAG, "Policy data is up to date. No need to update.");

                                // Update lastUpdated to current time format 2024-03-19T21:25:27.937Z API 24
                                @SuppressLint("SimpleDateFormat") SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
                                sdf.setTimeZone(TimeZone.getTimeZone("UTC")); // Ensure the time is in UTC
                                policyData.lastUpdated = sdf.format(Calendar.getInstance().getTime());
                                db.policyDataDAO().refreshPolicyData(policyData.lastUpdated, policyData.actionId);
                                callbackActivity.onCallback(true);

                        } else {
                                AppLog.i(TAG, "Policy data is outdated. Updating...");
                                db.policyDataDAO().delete(); // Synchronously delete old policy data
                                db.policyDataDAO().insertPolicyData(policyData); // Synchronously insert new policy data
                                AppLog.i(TAG, "Policy data updated successfully.");

                                // Perform permission check
                                performPermissionCheck(policyData, callbackActivity, context);

                                // Register background service
                                int intervalCheckin = policyData.checkinInterval;
                                WorkScheduler.scheduleFleetCheckinWorker(context, intervalCheckin, TimeUnit.SECONDS);
                                int intervalPut = policyData.putInterval;
                                WorkScheduler.scheduleElasticsearchWorker(context, 5, TimeUnit.SECONDS); // Run almost immediately

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
                        AppLog.w(TAG, "Checkin to Fleet failed with code: " + response.code() +  ". Context: " + response.toString() + " Response: " + response.errorBody().string());

                    } catch (IOException e) {
                        AppLog.e(TAG, "Checkin to Fleet failed with code: " + response.code() +  ". Context: " + response.toString() + " (Error body could not be read)");
                    }
                    callbackActivity.onCallback(false);
                }
            }

            @Override
            public void onFailure(@NonNull Call<FleetCheckinResponse> call, Throwable t) {
                if (t instanceof SocketTimeoutException) {
                    AppLog.i(TAG, "Checkin successful but no new actions were available (timeout).");
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
                    AppLog.e(TAG, "Unhandled exception in checkin: " + t.getMessage());
                    callbackActivity.onCallback(false);
                }

            }
        });

    }

    /**
     * Performs a permission check based on the policy data received from the Fleet server.
     * This method checks if all required permissions are granted and requests them if necessary.
     * If the app is in the foreground, a dialog is shown to request permissions. Otherwise, a
     * notification is displayed to prompt the user to grant the required permissions.
     *
     * @param policyData The policy data received from the Fleet server.
     * @param callbackActivity Callback to handle UI updates based on the permission check result.
     * @param context Application context.
     */
    private void performPermissionCheck(PolicyData policyData, StatusCallback callbackActivity, Context context) {
        List<String> permissions = new ArrayList<>();
        List<String> compPermissions = new ArrayList<>();

        // Permission handling. Gather all required permissions from enabled components first
        for (String componentPath : policyData.paths.split(",")) {
            try{
                // Remove everything behind the first "." to get the component name
                String componentName = componentPath.split("\\.")[0];

                String subComponent = "";
                if(componentPath.split("\\.").length >= 2) {
                    subComponent = componentPath.split("\\.")[1];
                }

                Component component = ComponentFactory.createInstance(componentName);
                compPermissions = component.getRequiredPermissions();
                if (compPermissions == null) {
                    AppLog.d(TAG, "Component " + componentName + " requires no permissions.");
                    continue;
                }
                AppLog.d(TAG, "Component " + componentName + " requires permissions: " + compPermissions);
                permissions.addAll(compPermissions);

            } catch (Exception e){
                if (e instanceof IllegalArgumentException && Objects.requireNonNull(e.getMessage()).contains("not found")) {
                    AppLog.w(TAG, "Component path " + componentPath + " defined in policy but app does not support it");
                } else {
                    AppLog.e(TAG, "Error while processing required permissions: " + e.getMessage());
                    writeDialog("Checkin failed. Error while processing required permissions: " + e.getMessage(), false);
                    callbackActivity.onCallback(false);
                    return;
                }
            }
        }

        // Now add notification permission if not already present as these are required for notifications
        if (!permissions.contains("android.permission.ACCESS_NOTIFICATION_POLICY")) {
            // Add first to ensure it's the first permission to be requested
            permissions.add(0, "android.permission.POST_NOTIFICATIONS");
        }

        AppLog.i(TAG, "Gathered permissions that are required by components: " + permissions);
        // Check if all permissions are granted
        if (!permissions.isEmpty()) {
            String[] permissionsArray = permissions.toArray(new String[0]);

            if (isAppInForeground(context)) {
                Intent intent = new Intent(context, PermissionRequestActivity.class);
                intent.putExtra("permissions", permissionsArray);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
            } else if (!PermissionRequestActivity.hasPermissions(context, permissionsArray)) {
                AppLog.w(TAG, "Permissions are missing. Showing notification to request permissions.");
                PermissionRequestActivity.showPermissionNotification(context);
            }
        }
    }

    /**
     * Acknowledges the policy data received from the Fleet server.
     *
     * @param policyData The policy data to acknowledge.
     * @return true if the acknowledgment was successful, false otherwise.
     */
    private boolean ackPolicy(PolicyData policyData) {
        AppLog.i(TAG, "Acknowledging policy data...");
        writeDialog("Acknowledging policy data...", true);

        // Create ack request
        AckRequest ackRequest = new AckRequest("ACTION_RESULT", "ACKNOWLEDGED", enrollmentData.agentId, policyData.actionId, "Policy update success.");
        fleetApi.postAck("ApiKey " + enrollmentData.accessApiKey, enrollmentData.agentId, ackRequest).enqueue(new Callback<AckResponse>() {
            @Override
            public void onResponse(@NonNull Call<AckResponse> call, @NonNull Response<AckResponse> response) {
                AppLog.d(TAG, "Got Ack Response from Fleet Server: " + new Gson().toJson(response.body()));

                if (response.isSuccessful() && response.body() != null && response.body().getItems() != null && response.body().getItems().length > 0) {
                    if(response.body().getItems()[0].getStatus() == 200) {
                        AppLog.i(TAG, "Policy data acknowledged successfully.");
                        writeDialog("Policy data acknowledged successfully.", true);
                    } else {
                        writeDialog("Policy data acknowledgement failed with code: " + response.body().getItems()[0].getStatus() +  ". Error: " + response.body().getItems()[0].getMessage(), false);
                        AppLog.w(TAG, "Policy data acknowledgement failed with code: " + response.body().getItems()[0].getStatus() +  ". Error: " + response.body().getItems()[0].getMessage());
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
                        AppLog.w(TAG, "Policy data acknowledgement failed with code: " + response.code() +  ". Context: " + response.toString() + " Response: " + response.errorBody().string());

                    } catch (Exception e) {
                        AppLog.e(TAG, "Policy data acknowledgement failed with code: " + response.code() +  ". Context: " + response.toString() + " (Error body could not be read)");
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<AckResponse> call, Throwable t) {
                // Handle failure
                writeDialog("Unhandled exception in policy data acknowledgement: " + t.getMessage(), false);
                AppLog.e(TAG, "Unhandled exception in policy data acknowledgement: " + t.getMessage());
            }
        });

        return true;
    }

    /**
     * Updates the UI based on the status of the check-in or acknowledgment process.
     * This method displays a dialog or updates a TextView with the provided message.
     * If the operation was successful, the message is displayed in green text. If it
     * failed, the message is displayed in red text (except for the dialog).
     *
     * If the operation was called from the background, no UI updates are performed.
     *
     * @param message The message to display.
     * @param success Indicates whether the operation was successful.
     */
    private void writeDialog(String message, boolean success) {
        if(dialog != null && dialog.isShowing()){
            dialog.setMessage(message);
        }

        if(dialog == null && tStatus != null){
            if(success){
                tStatus.setText("Checkin: " + message);
            } else {
                tStatus.setTextColor(tStatus.getResources().getColor(android.R.color.holo_red_dark));
                tStatus.setText("Checkin Error: " + message);
            }
        }
    }

    /**
     * Parses the policy data from the Fleet server's check-in response.
     *
     * @param response The check-in response from the Fleet server.
     * @return The parsed PolicyData object or null if parsing failed.
     */
    private PolicyData parsePolicy(FleetCheckinResponse response) {
        String TAG_PARSE = TAG + " - parsePolicy";

        if (response.getActions() == null || response.getActions().isEmpty()) {
            AppLog.e(TAG_PARSE, "No actions in response.");
            return null;
        }

        FleetCheckinResponse.Action action = response.getActions().get(0);

        if (action == null || action.getData() == null || action.getData().getPolicy() == null) {
            AppLog.e(TAG_PARSE, "Action, Action Data, or Policy is null.");
            return null;
        }
        
        FleetCheckinResponse.Action.PolicyData.Policy policy = action.getData().getPolicy();
        PolicyData policyData = new PolicyData();
        policyData.createdAt = action.getCreatedAt();
        policyData.revision = policy.getRevision();

        if (policy.getAgent() == null || policy.getAgent().getProtection() == null) {
            AppLog.e(TAG_PARSE, "Agent or Protection data is missing.");
            return null;
        }

        policyData.protectionEnabled = policy.getAgent().getProtection().getEnabled();
        policyData.uninstallTokenHash = policy.getAgent().getProtection().getUninstallTokenHash();

        if (policy.getInputs() == null || policy.getInputs().isEmpty() || policy.getInputs().get(0).getStreams() == null || policy.getInputs().get(0).getStreams().isEmpty()) {
            AppLog.e(TAG_PARSE, "Inputs or Streams data is missing.");
            return null;
        }

        FleetCheckinResponse.Action.Input input = policy.getInputs().get(0);
        policyData.inputName = input.getName();

        // Streams also contains custom settings
        FleetCheckinResponse.Action.Input.Stream stream = input.getStreams().get(0);
        policyData.allowUserUnenroll = stream.getAllowUserUnenroll();
        policyData.dataStreamDataset = stream.getDataStream().getDataset();
        policyData.ignoreOlder = stream.getIgnoreOlder();
        policyData.useBackoff = stream.getUseBackoff();
        policyData.maxBackoffInterval = timeIntervalToSeconds(stream.getMaxBackoffInterval());
        policyData.backoffOnEmptyBuffer = stream.getBackoffOnEmptyBuffer();

        // Parse Xm or Xs etc. to seconds
        int checkinIntervalSeconds = timeIntervalToSeconds(stream.getCheckinInterval());
        int putIntervalSeconds = timeIntervalToSeconds(stream.getPutInterval());


        policyData.checkinInterval = checkinIntervalSeconds;
        policyData.backoffCheckinInterval = checkinIntervalSeconds;

        policyData.putInterval = putIntervalSeconds;
        policyData.backoffPutInterval = putIntervalSeconds;

        policyData.maxDocumentsPerRequest = stream.getMaxDocumentsPerRequest();

        if(stream.getPaths() == null || stream.getPaths().isEmpty()){
            AppLog.e(TAG_PARSE, "Path data is missing.");
            return null;
        }

        policyData.paths = String.join(",", stream.getPaths()); // Concatenate paths



        if (policy.getOutputs() == null || policy.getOutputs().isEmpty()) {
            AppLog.e(TAG_PARSE, "Outputs data is missing.");
            return null;
        }

        Map.Entry<String, FleetCheckinResponse.Action.PolicyData.Policy.Output> entry = policy.getOutputs().entrySet().iterator().next();
        FleetCheckinResponse.Action.PolicyData.Policy.Output output = entry.getValue();

        if(output == null){
            AppLog.e(TAG_PARSE, "Output data is missing.");
            return null;
        }

        if(output.getApiKey() == null || output.getHosts() == null || output.getSslCaTrustedFingerprint() == null){
            AppLog.e(TAG_PARSE, "Output data is incomplete.");
            return null;
        }

        policyData.apiKey = output.getApiKey();
        policyData.hosts = String.join(",", output.getHosts()); // Concatenate hosts
        policyData.sslCaTrustedFingerprint = output.getSslCaTrustedFingerprint();
        policyData.sslCaTrustedFull = output.getSslCertificateAuthorities().stream().findFirst().orElse(null);


        if(action.getId() == null){
            AppLog.e(TAG_PARSE, "Action ID is missing.");
            return null;
        }
        policyData.actionId = action.getId();

        // Update lastUpdated to current time format 2024-03-19T21:25:27.937Z API 24
        @SuppressLint("SimpleDateFormat") SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        sdf.setTimeZone(TimeZone.getTimeZone("UTC")); // Ensure the time is in UTC
        policyData.lastUpdated = sdf.format(Calendar.getInstance().getTime());

        return policyData;
    }

    /**
     * Converts a time interval string (e.g., "5m", "1h", "2d") to seconds.
     * Opposite of {@link #secondsToTimeInterval(int)}.
     *
     * @param interval The time interval string to convert. Supported formats: "Xw", "Xd", "Xh", "Xm", "Xs".
     * @return The time interval in seconds.
     */
    public static int timeIntervalToSeconds(String interval) {
        if (interval == null || interval.trim().isEmpty()) {
            AppLog.w(TAG, "Time interval " + interval + " is null or empty. Defaulting to 60 seconds.");
            return 60;
        }

        int seconds = 0;
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("(\\d+w)|(\\d+d)|(\\d+h)|(\\d+m)|(\\d+s)").matcher(interval);
        while (matcher.find()) {
            if (matcher.group().endsWith("w")) {
                seconds += Integer.parseInt(matcher.group().replaceAll("[^0-9]", "")) * 604800; // 7 days
            } else if (matcher.group().endsWith("d")) {
                seconds += Integer.parseInt(matcher.group().replaceAll("[^0-9]", "")) * 86400; // 24 hours
            } else if (matcher.group().endsWith("h")) {
                seconds += Integer.parseInt(matcher.group().replaceAll("[^0-9]", "")) * 3600;
            } else if (matcher.group().endsWith("m")) {
                seconds += Integer.parseInt(matcher.group().replaceAll("[^0-9]", "")) * 60;
            } else if (matcher.group().endsWith("s")) {
                seconds += Integer.parseInt(matcher.group().replaceAll("[^0-9]", ""));
            }
        }

        if (seconds == 0) {
            AppLog.w(TAG, "Unknown time interval format: " + interval + ". Defaulting to 60 seconds.");
            return 60;
        }

        return seconds;
    }

    /**
     * Converts seconds to a human-readable time interval string (e.g., "5m", "1h").
     * Opposite of {@link #timeIntervalToSeconds(String)}.
     *
     * @param totalSeconds The number of seconds to convert.
     * @return A human-readable time interval string.
     */
    public static String secondsToTimeInterval(int totalSeconds) {
        if (totalSeconds <= 0) {
            return "0s"; // Handle non-positive inputs
        }

        int weeks = totalSeconds / 604800;
        int days = (totalSeconds % 604800) / 86400;
        int hours = (totalSeconds % 86400) / 3600;
        int minutes = (totalSeconds % 3600) / 60;
        int seconds = totalSeconds % 60;

        StringBuilder sb = new StringBuilder();
        if (weeks > 0) {
            sb.append(weeks).append("w");
        }
        if (days > 0) {
            sb.append(days).append("d");
        }
        if (hours > 0) {
            sb.append(hours).append("h");
        }
        if (minutes > 0) {
            sb.append(minutes).append("m");
        }
        if (seconds > 0 || sb.length() == 0) { // Include seconds if it's the only unit or add it to existing units
            sb.append(seconds).append("s");
        }

        return sb.toString();
    }

    /**
     * Checks whether the application is currently running in the foreground.
     *
     * @param context Application context.
     * @return true if the app is in the foreground, false otherwise.
     */
    public static boolean isAppInForeground(Context context) {
        ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningAppProcessInfo> appProcesses = activityManager.getRunningAppProcesses();
        if (appProcesses == null) {
            return false;
        }
        final String packageName = context.getPackageName();
        for (ActivityManager.RunningAppProcessInfo appProcess : appProcesses) {
            if (appProcess.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND && appProcess.processName.equals(packageName)) {
                return true;
            }
        }
        return false;
    }

}



