package de.swiftbird.elasticandroid;

import com.google.gson.annotations.SerializedName;
import java.util.List;

/**
 * Represents the response received from the Fleet server after enrolling an agent.
 * This class contains details about the enrollment status and the agent's access key that is later used for communication.
 * with the Fleet server.
 */
public class FleetEnrollResponse {
    private String action;
    private Item item;

    // Constructor (mostly for testing)
    public FleetEnrollResponse(String action, Item item) {
        this.action = action;
        this.item = item;
    }

    public static class Item {
        @SerializedName("access_api_key")
        private String accessApiKey;

        @SerializedName("access_api_key_id")
        private String accessApiKeyId;

        @SerializedName("active")

        private Boolean active;

        @SerializedName("enrolled_at")
        private String enrolledAt;

        @SerializedName("id")
        private String id;

        @SerializedName("policy_id")
        private String policyId;

        @SerializedName("status")
        private String status;

        @SerializedName("tags")
        private List<String> tags;

        @SerializedName("type")
        private String type;

        /**
         * Constrctor only used for testing
         * @param accessApiKey The access API key
         * @param accessApiKeyId The access API key ID
         * @param active The active status
         * @param enrolledAt The enrollment date
         * @param id The ID
         * @param policyId The policy ID
         * @param status The status
         * @param tags The tags
         * @param type The type
         */
        public Item(String accessApiKey, String accessApiKeyId, Boolean active, String enrolledAt, String id, String policyId, String status, List<String> tags, String type) {
            this.accessApiKey = accessApiKey;
            this.accessApiKeyId = accessApiKeyId;
            this.active = active;
            this.enrolledAt = enrolledAt;
            this.id = id;
            this.policyId = policyId;
            this.status = status;
            this.tags = tags;
            this.type = type;
        }

        // Getters and Setters

        public String getAccessApiKey() {
            return accessApiKey;
        }

        public String getAccessApiKeyId() {
            return accessApiKeyId;
        }

        public Boolean getActive() {
            return active;
        }

        public String getEnrolledAt() {
            return enrolledAt;
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getPolicyId() {
            return policyId;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }
    }

    // Getters and Setters
    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public Item getItem() {
        return item;
    }
}
