package de.swiftbird.elasticandroid;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class FleetCheckinRequest {
    @SerializedName("status")
    private String status;

    @SerializedName("ack_token")
    private String ackToken;

    @SerializedName("local_metadata")
    private AgentMetadata metadata;

    @SerializedName("message")
    private String message;

    @SerializedName("components")
    private List<String> components;

    @SerializedName("upgrade_details")
    private String upgradeDetails;

    // Constructors, getters, and setters
    public FleetCheckinRequest(String status, String ackToken, AgentMetadata metadata, String message, List<String> components, String upgradeDetails) {
        this.status = status;
        this.ackToken = ackToken;
        this.metadata = metadata;
        this.message = message;
        //this.components = components;
        //this.upgradeDetails = upgradeDetails;
    }

    // Getters and setters...
}
