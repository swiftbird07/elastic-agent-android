package de.swiftbird.elasticandroid;

import android.content.Context;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.room.Room;

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
    private TextView tError;

    public EnrollmentRepository(Context context, String serverUrl, String token, boolean checkCert, TextView tError) {
        this.context = context;
        // Initialize Retrofit instance
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(serverUrl)
                .client(getOkHttpClient(checkCert, token))
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        fleetApi = retrofit.create(FleetApi.class);
        this.tError = tError;
    }

    public LiveData<EnrollmentResponse> enrollAgent(EnrollmentRequest request) {
        Log.i(TAG, "Starting enrollment process...");
        Log.d(TAG, "User provided Server URL: " + request.getServerUrl());
        Log.d(TAG, "User provided Hostname: " + request.getHostname());
        Log.d(TAG, "User provided Tags: " + request.getTags());
        //Log.i(TAG, "User provided Token: " + request.getToken())

        // First call API info endpoint:
        Log.i(TAG, "Requesting API details...");
        final MutableLiveData<ApiResponse> liveDataApi = new MutableLiveData<ApiResponse>();

        fleetApi.getApiInfo().enqueue(new Callback<ApiResponse>() {
            @Override
            public void onResponse(Call<ApiResponse> call, Response<ApiResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Log.i(TAG, "API version: " + response.body().getData().getApi_version());
                    Log.i(TAG, "API revision: " + response.body().getData().getRevision());
                    Log.i(TAG, "Fleet hostname: " + response.body().getData().getHostname());
                } else {
                    try {
                        tError.setText("Could not communicate with Fleet API - Message: " + response.errorBody().string());
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    Log.w(TAG, "Initial API request failed. Stopping enrollment.");
                    liveDataApi.postValue(null);
                }
            }


            @Override
            public void onFailure(Call<ApiResponse> call, Throwable t) {
                // Handle failure
                tError.setText("Could not communicate with Fleet API - Error: " + t.getMessage());
                Log.e(TAG, "Unhandled exception in initial API request. Stopping enrollment. Response: " + call.toString() + " Error: " + t.toString());
                liveDataApi.postValue(null); // Simplified for brevity
            }
        });

        if(liveDataApi.getValue() == null) return new LiveData<EnrollmentResponse>(){};


        // Use MutableLiveData to post the result from the network operation
        Log.i(TAG, "Requesting enrollment...");
        final MutableLiveData<EnrollmentResponse> liveData = new MutableLiveData<EnrollmentResponse>();

        fleetApi.enrollAgent(request).enqueue(new Callback<EnrollmentResponse>() {
            @Override
            public void onResponse(Call<EnrollmentResponse> call, Response<EnrollmentResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Log.i(TAG, "Enrollment successful.");
                    liveData.postValue(response.body());
                    Log.i(TAG, "Saving Enrollment data to db...");
                    saveEnrollmentInfo(request, response.body());
                } else {
                    // Handle errors
                    tError.setText("Could not complete Fleet enrollment - Response: " + response.errorBody().toString());
                    Log.w(TAG, "Enrollment request failed. Stopping enrollment.");
                    liveData.postValue(null); // Simplified for brevity
                }
            }

            @Override
            public void onFailure(Call<EnrollmentResponse> call, Throwable t) {
                // Handle failure
                tError.setText("Could not complete Fleet enrollment - Error: " + t.getMessage());
                Log.e(TAG, "Unhandled exception in initial enrollment request. Stopping enrollment. Response: " + call.toString() + " Error: " + t.toString());
                liveData.postValue(null); // Simplified for brevity
            }
        });

        return liveData;
    }

    public void saveEnrollmentInfo(EnrollmentRequest request, EnrollmentResponse response) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                EnrollmentData enrollmentInfo = new EnrollmentData();
                enrollmentInfo.agentId = response.getData().getAgentId();
                enrollmentInfo.serverUrl = request.getServerUrl();
                // Set other fields as needed

                AppDatabase db = Room.databaseBuilder(context.getApplicationContext(),
                        AppDatabase.class, "enrollment-data").build();
                db.EnrollmentDataDAO().insertEnrollmentInfo(enrollmentInfo);
                Log.i(TAG, "Saving Enrollment data to db successful.");
            }
        }).start();
    }



    private static OkHttpClient getOkHttpClient(boolean checkCA, String token) {
        Interceptor authInterceptor = new Interceptor() {
            @Override
            public okhttp3.Response intercept(Chain chain) throws IOException {
                Request originalRequest = chain.request();
                Request newRequest = originalRequest.newBuilder()
                        .header("Authorization", "Bearer " + token)
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
