package de.swiftbird.elasticandroid;

import com.google.gson.annotations.SerializedName;

/**
 * AckResponse models the JSON structure of a response from a fleet management system following an acknowledgment request.
 * This class is prepared for Retrofit to easily deserialize the received JSON into a structured Java object. It specifically
 * captures details about the action being acknowledged and the outcomes of such acknowledgments through an array of items,
 * each item containing a status code and a message indicative of the result of the acknowledgment process.
 */
public class AckResponse {
    @SerializedName("action")
    private String action; // Indicates the action type of the acknowledgment, e.g., "acks".

    @SerializedName("items")
    private Item[] items; // Array of individual acknowledgment outcomes.

    public String getAction() {
        return action;
    }

    public Item[] getItems() {
        return items;
    }

    public static class Item {
        @SerializedName("message")
        private String message; // Descriptive message of the acknowledgment outcome.

        @SerializedName("status")
        private int status; // HTTP status code representing the result of the acknowledgment.

        public String getMessage() {
            return message;
        }

        public int getStatus() {
            return status;
        }
    }
}
