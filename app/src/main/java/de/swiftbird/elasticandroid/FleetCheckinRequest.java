package de.swiftbird.elasticandroid;

import com.google.gson.annotations.SerializedName;
import java.util.List;

/**
 * Constructs a check-in request for the Fleet server, encapsulating the agent's
 * current status, local metadata, and additional information for potential actions
 * like upgrades. This class plays a pivotal role in the periodic communication between
 * the agent and the Fleet server, enabling the server to assess the agent's health,
 * issue new policies, or respond to the agent's state.
 */
public class FleetCheckinRequest {
    @SerializedName("status")
    private final String status;

    @SerializedName("ack_token")
    private final String ackToken;

    @SerializedName("local_metadata")
    private final AgentMetadata metadata;

    @SerializedName("message")
    private final String message;

    @SerializedName("components")
    private List<String> components;

    @SerializedName("upgrade_details")
    private String upgradeDetails;

    /**
     * Constructs a FleetCheckinRequest object with the specified status, acknowledgment token,
     * agent metadata, and message. This constructor is used to create a check-in request
     * to be sent to the Fleet server, updating the agent's status and metadata.
     *
     * @param status The current status of the agent.
     * @param ackToken The acknowledgment token for the agent.
     * @param metadata The agent's local metadata.
     * @param message An optional message to include in the check-in request.
     */
    public FleetCheckinRequest(String status, String ackToken, AgentMetadata metadata, String message) {
        this.status = status;
        this.ackToken = ackToken;
        this.metadata = metadata;
        this.message = message;
    }

}
