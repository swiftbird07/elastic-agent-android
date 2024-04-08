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
         * @param accessApiKey
         * @param accessApiKeyId
         * @param active
         * @param enrolledAt
         * @param id
         * @param policyId
         * @param status
         * @param tags
         * @param type
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

        public void setAccessApiKey(String accessApiKey) {
            this.accessApiKey = accessApiKey;
        }

        public String getAccessApiKeyId() {
            return accessApiKeyId;
        }

        public void setAccessApiKeyId(String accessApiKeyId) {
            this.accessApiKeyId = accessApiKeyId;
        }

        public Boolean getActive() {
            return active;
        }

        public void setActive(Boolean active) {
            this.active = active;
        }

        public String getEnrolledAt() {
            return enrolledAt;
        }

        public void setEnrolledAt(String enrolledAt) {
            this.enrolledAt = enrolledAt;
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

        public void setPolicyId(String policyId) {
            this.policyId = policyId;
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

    public void setItem(Item item) {
        this.item = item;
    }

}
