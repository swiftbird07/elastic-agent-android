package de.swiftbird.elasticandroid;

import com.google.gson.annotations.SerializedName;

public class AckResponse {
    // Retrofit will use this class to receive the ack response from fleet using this structure:
    /*
    {
    "action": "acks",
    "items": [
        {
            "message": "OK",
            "status": 200
        }
        ]
    }
     */

    @SerializedName("action")
    private String action;

    @SerializedName("items")
    private Item[] items;


    public String getAction() {
        return action;
    }

    public Item[] getItems() {
        return items;
    }

    public class Item {

        @SerializedName("message")
        private String message;

        @SerializedName("status")
        private int status;

        public String getMessage() {
            return message;
        }

        public int getStatus() {
            return status;
        }
    }

}
