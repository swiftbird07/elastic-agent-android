package de.swiftbird.elasticandroid;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.Path;

public interface FleetApi {

    @GET("/api/status")
    Call<FleetStatusResponse> getFleetStatus();

    @POST("/api/fleet/agents/enroll")
    Call<FleetEnrollResponse> enrollAgent(@Header("Authorization") String enrollmentTokenHeader, @Body FleetEnrollRequest enrollRequest);

    @POST("/api/fleet/agents/{id}/checkin")
    Call<FleetCheckinResponse> postCheckin(
            @Header("Authorization") String accessApiKeyHeader,
            @Path("id") String id,
            @Body FleetCheckinRequest checkinRequest);

    @POST("/api/fleet/agents/{id}/acks")
    Call<AckResponse> postAck(
            @Header("Authorization") String accessApiKeyHeader,
            @Path("id") String id,
            @Body AckRequest ackRequest);
}