package de.swiftbird.elasticandroid;

import android.content.Context;
import android.util.Log;
import android.widget.TextView;


import androidx.room.Room;

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

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import okhttp3.OkHttpClient;

public class EnrollmentRepository {

    private final Context context;
    private FleetApi fleetApi;
    private static final String TAG = "EnrollmentRepository";
    private TextView tError, tStatus;
    private String token;


    public EnrollmentRepository(Context context, String serverUrl, String token, boolean checkCert, TextView tStatus, TextView tError) {
        this.context = context;
        // Initialize Retrofit instance
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(serverUrl)
                .client(getOkHttpClient(checkCert))
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        fleetApi = retrofit.create(FleetApi.class);
        this.token = token;

        this.tError = tError;
        this.tStatus = tStatus;
    }

    public void enrollAgent(AppEnrollmentRequest request, de.swiftbird.elasticandroid.Callback callback) {
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


                        fleetApi.enrollAgent("ApiKey " + token, enrollRequest).enqueue(new Callback<FleetEnrollResponse>() {
                            @Override
                            public void onResponse(Call<FleetEnrollResponse> call, Response<FleetEnrollResponse> response) {
                                if (response.isSuccessful()) {
                                    FleetEnrollResponse enrollResponse = response.body();

                                    if(enrollResponse != null && enrollResponse.getItem() != null && enrollResponse.getItem().getId() != null){
                                        Log.i(TAG, "Enrollment successful. Agent ID: " + enrollResponse.getItem().getId());
                                        tStatus.setText("Enrollment successful. Agent ID: " + enrollResponse.getItem().getId());

                                        saveEnrollmentInfo(request, enrollResponse);

                                        // Callback to the activity
                                        callback.onCallback(true);

                                    } else {
                                        tError.setText("Enrollment failed because of invalid response from Fleet Server.");
                                        Log.e(TAG, "Enrollment failed because of invalid response from Fleet Server. Context: " + response.toString());
                                        callback.onCallback(false);
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
                                    callback.onCallback(false);
                                }
                            }

                            @Override
                            public void onFailure(Call<FleetEnrollResponse> call, Throwable t) {
                                tError.setText("Enrollment failed with error: " + t.getMessage());
                                Log.e(TAG, "Enrollment failed with error: " + t.getMessage());
                                callback.onCallback(false);
                            }
                        });


                    } else {
                        tError.setText("Fleet server is not healthy - Status: " + response.body().getStatus());
                        Log.w(TAG, "Fleet Server is not healthy. Stopping enrollment.");
                        callback.onCallback(false);
                    }
                } else {
                    tError.setText("Could not communicate with fleet server - Check server URL and try again. Status Code: " + response.code());
                    Log.e(TAG, "Unhandled externally caused exception in initial Fleet Server request. Stopping enrollment. Response: " + response.toString());
                    callback.onCallback(false);
                }
            }

            @Override
            public void onFailure(Call<FleetStatusResponse> call, Throwable t) {
                // Handle failure
                tError.setText("Could not communicate with Fleet Server - Error: " + t.getMessage());
                Log.e(TAG, "Unhandled exception in initial Fleet Server request. Stopping enrollment. Response: " + call.toString() + " Error: " + t.toString());
                callback.onCallback(false);
            }
        });


    }

    public void saveEnrollmentInfo(AppEnrollmentRequest request, FleetEnrollResponse response) {
        new Thread(new Runnable() {
            @Override
            public void run() {

                FleetEnrollResponse.Item item = response.getItem();

                EnrollmentData enrollmentInfo = new EnrollmentData();

                enrollmentInfo.id = 1; // We only have one enrollment info in the database
                enrollmentInfo.action = response.getAction();
                enrollmentInfo.accessApiKey = item.getAccessApiKey();
                enrollmentInfo.accessApiKeyId = item.getAccessApiKeyId();
                enrollmentInfo.active = item.getActive();
                enrollmentInfo.enrolledAt = item.getEnrolledAt();
                enrollmentInfo.policyId = item.getPolicyId();
                enrollmentInfo.status = item.getStatus();
                enrollmentInfo.isEnrolled = true;
                enrollmentInfo.hostname = request.getHostname();
                enrollmentInfo.agentId = item.getId();


                AppDatabase db = Room.databaseBuilder(context.getApplicationContext(),
                        AppDatabase.class, "enrollment-data").build();
                db.enrollmentDataDAO().insertEnrollmentInfo(enrollmentInfo);
                Log.i(TAG, "Saving Enrollment data to db successful.");
            }
        }).start();
    }



    private static OkHttpClient getOkHttpClient(boolean checkCA) {
        Interceptor authInterceptor = new Interceptor() {
            @Override
            public okhttp3.Response intercept(Chain chain) throws IOException {
                Request originalRequest = chain.request();
                Request newRequest = originalRequest.newBuilder()
                        .header("User-Agent", "elastic agent " + BuildConfig.AGENT_VERSION)
                        .build();
                return chain.proceed(newRequest);
            }
        };

        if (checkCA){
            return new OkHttpClient.Builder().addInterceptor(authInterceptor).build();
        } else {

            try {
                // Create a trust manager that does not validate certificate chains
                final TrustManager[] trustAllCerts = new TrustManager[]{
                        new X509TrustManager() {
                            @Override
                            public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String authType) throws CertificateException {
                            }

                            @Override
                            public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType) throws CertificateException {
                            }

                            @Override
                            public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                                return new java.security.cert.X509Certificate[]{};
                            }
                        }
                };

                // Install the all-trusting trust manager
                final SSLContext sslContext = SSLContext.getInstance("SSL");
                sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
                // Create an ssl socket factory with our all-trusting manager
                final SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();

                OkHttpClient.Builder builder = new OkHttpClient.Builder();
                builder.sslSocketFactory(sslSocketFactory);
                builder.hostnameVerifier(new HostnameVerifier() {
                    @Override
                    public boolean verify(String hostname, SSLSession session) {
                        return true;
                    }
                });
                builder.authenticator(new Authenticator() {
                    @Override
                    public Request authenticate(Route route, okhttp3.Response response) throws IOException {
                        return null;
                    }
                });

                OkHttpClient okHttpClient = builder.addInterceptor(authInterceptor).build();
                return okHttpClient;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

}
