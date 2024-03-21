package de.swiftbird.elasticandroid;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;

public interface FleetApi {

    @GET("/api/status")
    Call<FleetStatusResponse> getFleetStatus();

    @POST("/api/fleet/agents/enroll")
    Call<FleetEnrollResponse> enrollAgent(@Header("Authorization") String apiKey, @Body FleetEnrollRequest enrollRequest);


}