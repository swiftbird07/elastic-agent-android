package de.swiftbird.elasticandroid;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;

public interface FleetApi {
    @POST("agents/enroll")
    Call<EnrollmentResponse> enrollAgent(@Body EnrollmentRequest request);

    @GET("/")
    Call<ApiResponse> getApiInfo();

}