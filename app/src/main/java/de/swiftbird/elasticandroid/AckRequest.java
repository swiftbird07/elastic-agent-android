package de.swiftbird.elasticandroid;

import com.google.gson.annotations.SerializedName;

/**
 * Represents an acknowledgment request to be sent to a fleet management system.
 * This class models the structure of the data required by Retrofit to perform the acknowledgment action.
 * It encapsulates a list of events, with each event capturing details about the acknowledgment action.
 */
public class AckRequest {
    /**
     * Array of events to be acknowledged. Each event contains details such as type, subtype, agent ID, action ID, and a message.
     */
    @SerializedName("events")
    private final Event[] events;

    /**
     * Represents a single event in the acknowledgment request.
     * This nested class details the structure of an event, including its type, subtype, agent and action IDs, and a descriptive message.
     */
    public static class Event {
        @SerializedName("type")
        private String type;

        @SerializedName("subtype")
        private String subtype;

        @SerializedName("agent_id")
        private String agentId;

        @SerializedName("action_id")
        private String actionId;

        @SerializedName("message")
        private String message;
    }

    /**
     * Constructs an acknowledgment request with specified details for a single event.
     *
     * @param type      The type of the event, e.g., "ACTION_RESULT".
     * @param subtype   The subtype of the event, e.g., "ACKNOWLEDGED".
     * @param agent_id  The unique identifier of the agent.
     * @param action_id The unique identifier of the action being acknowledged.
     * @param message   A descriptive message about the acknowledgment, e.g., "Policy update success."
     */
    public AckRequest(String type, String subtype, String agent_id, String action_id, String message) {
        this.events = new Event[]{new Event()};
        events[0].type = type;
        events[0].subtype = subtype;
        events[0].agentId = agent_id;
        events[0].actionId = action_id;
        events[0].message = message;
    }
}
