package de.swiftbird.elasticandroid;

import com.google.gson.annotations.SerializedName;

/**
 * Represents an enrollment request sent to the Fleet server to enroll an agent.
 * This class contains all details required by the Fleet server to authenticate
 * and authorize the agent's enrollment request.
 */
public class FleetEnrollRequest {
    @SerializedName("type")
    private String type;

    @SerializedName("metadata")
    private AgentMetadata metadata;

    @SerializedName("enrollment_id")
    private String enrollmentId;

    @SerializedName("shared_id")
    private String sharedId;

    // Getters and Setters
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setMetadata(AgentMetadata metadata) {
        this.metadata = metadata;
    }

}
