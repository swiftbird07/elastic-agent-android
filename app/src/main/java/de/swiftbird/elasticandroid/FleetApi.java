package de.swiftbird.elasticandroid;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.Path;

/**
 * Defines the HTTP API for interacting with the Fleet server. This interface includes methods
 * for checking the Fleet server's status, enrolling an agent with the server, and managing
 * agent check-ins and task acknowledgments.
 */
public interface FleetApi {

    /**
     * Retrieves the current status of the Fleet server.
     *
     * @return A {@link Call} object with {@link FleetStatusResponse} expected upon successful request.
     */
    @GET("/api/status")
    Call<FleetStatusResponse> getFleetStatus();

    /**
     * Enrolls an agent with the Fleet server using a provided enrollment token.
     *
     * @param enrollmentTokenHeader The 'Authorization' header containing the enrollment token.
     * @param enrollRequest The enrollment request details.
     * @return A {@link Call} object with {@link FleetEnrollResponse} expected upon successful enrollment.
     */
    @POST("/api/fleet/agents/enroll")
    Call<FleetEnrollResponse> enrollAgent(@Header("Authorization") String enrollmentTokenHeader, @Body FleetEnrollRequest enrollRequest);

    /**
     * Posts a check-in for an agent to the Fleet server, updating its status and receiving new tasks.
     *
     * @param accessApiKeyHeader The 'Authorization' header containing the agent's access API key.
     * @param id The unique identifier of the agent.
     * @param checkinRequest The check-in request details, including the agent's status and local metadata.
     * @return A {@link Call} object with {@link FleetCheckinResponse} expected upon successful check-in.
     */
    @POST("/api/fleet/agents/{id}/checkin")
    Call<FleetCheckinResponse> postCheckin(
            @Header("Authorization") String accessApiKeyHeader,
            @Path("id") String id,
            @Body FleetCheckinRequest checkinRequest);

    /**
     * Posts an acknowledgment from an agent to the Fleet server, indicating that a task has been executed.
     *
     * @param accessApiKeyHeader The 'Authorization' header containing the agent's access API key.
     * @param id The unique identifier of the agent.
     * @param ackRequest The acknowledgment request details, typically including the task's unique ID and execution status.
     * @return A {@link Call} object with {@link AckResponse} expected upon successful acknowledgment.
     */
    @POST("/api/fleet/agents/{id}/acks")
    Call<AckResponse> postAck(
            @Header("Authorization") String accessApiKeyHeader,
            @Path("id") String id,
            @Body AckRequest ackRequest);
}
