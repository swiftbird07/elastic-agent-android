package de.swiftbird.elasticandroid;

import com.google.gson.annotations.SerializedName;

/**
 * Represents an enrollment request sent to the Fleet server to enroll an agent.
 * This class contains all details required by the Fleet server to
 * authenticate and authorize the agent's enrollment request.
 */
public class FleetEnrollRequest {
    private String type;
    @SerializedName("metadata")
    private AgentMetadata metadata;
    private String enrollment_id;
    private String shared_id;


    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public AgentMetadata getMetadata() {
        return metadata;
    }

    public void setMetadata(AgentMetadata metadata) {
        this.metadata = metadata;
    }

    public String getEnrollment_id() {
        return enrollment_id;
    }

    public void setEnrollment_id(String enrollment_id) {
        this.enrollment_id = enrollment_id;
    }

    public String getShared_id() {
        return shared_id;
    }

    public void setShared_id(String shared_id) {
        this.shared_id = shared_id;
    }



}
