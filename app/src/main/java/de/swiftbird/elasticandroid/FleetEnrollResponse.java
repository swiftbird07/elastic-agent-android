package de.swiftbird.elasticandroid;
import com.google.gson.annotations.SerializedName;
import java.util.List;

public class FleetEnrollResponse {
    private String action;
    private Item item;

    public static class Item {
        @SerializedName("access_api_key")
        private String accessApiKey;

        @SerializedName("access_api_key_id")
        private String accessApiKeyId;

        @SerializedName("active")

        /* {
    "action": "created",
    "item": {
        "access_api_key": "TW1Gc1hZNEIydUdpYWJxV0tFT0I6T0ZQY3ptN0xUYlM2N2FvUDItOHJnUQ==",
        "access_api_key_id": "MmFsXY4B2uGiabqWKEOB",
        "actions": null,
        "active": true,
        "enrolled_at": "2024-03-20T19:52:33Z",
        "id": "b2fe83e2-fffc-4b14-85e2-315ec9ac9538",
        "local_metadata": null,
        "policy_id": "eb0088c0-e635-11ee-8207-1b9b3ac48589",
        "status": "online",
        "tags": [],
        "type": "PERMANENT",
        "user_provided_metadata": null
    }
} */
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
