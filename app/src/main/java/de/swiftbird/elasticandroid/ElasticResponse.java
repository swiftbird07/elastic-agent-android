package de.swiftbird.elasticandroid;

import com.google.gson.annotations.SerializedName;

public class ElasticResponse {
    private String action;
    private Item item;

    // Constructor (mostly for testing)
    public ElasticResponse(String action, Item item) {
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
    }
}