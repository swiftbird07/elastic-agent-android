package de.swiftbird.elasticandroid;

import com.google.gson.annotations.SerializedName;

public class AckRequest {
    // Retrofit will use this class to send the ack to fleet using this structure:
    /*
      "type": "ACTION_RESULT",
      "subtype": "ACKNOWLEDGED",
      "agent_id": "396b14b9-2e23-4694-a6bc-675a519bb8ca",
      "action_id": "policy:eb0088c0-e635-11ee-8207-1b9b3ac48589:5:1",
      "message": "Policy update success."

     */
    @SerializedName("events")
    private Event[] events;

    public class Event {
        @SerializedName("type")
        private String type;
        @SerializedName("subtype")
        private String subtype;
        @SerializedName("agent_id")
        private String agent_id;
        @SerializedName("action_id")
        private String action_id;
        @SerializedName("message")
        private String message;

    }

    public AckRequest(String type, String subtype, String agent_id, String action_id, String message) {
        this.events = new Event[]{new Event()};
        events[0].type = type;
        events[0].subtype = subtype;
        events[0].agent_id = agent_id;
        events[0].action_id = action_id;
        events[0].message = message;
    }

}
