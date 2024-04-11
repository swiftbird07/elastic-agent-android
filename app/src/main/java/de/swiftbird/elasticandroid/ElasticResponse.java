package de.swiftbird.elasticandroid;

import com.google.gson.annotations.SerializedName;

/**
 * Represents a response from Elasticsearch, encapsulating details about the outcome
 * of an operation, such as indexing a document or executing a bulk request. It includes
 * information about any errors that occurred, as well as metrics like the time taken
 * for the operation to complete.
 */
public class ElasticResponse {

    private final String errors;
    private Error error;

    /**
     * Inner class representing detailed error information from Elasticsearch,
     * including the type of error and a descriptive reason.
     */
    public static class Error {
        @SerializedName("type")
        private String type;

        @SerializedName("reason")
        private String reason;

        public String getType() {
            return type;
        }

        public String getReason() {
            return reason;
        }
    }

    /**
     * Constructs an instance of ElasticResponse, primarily for testing purposes,
     * allowing manual creation of response objects.
     *
     * @param errors A raw error message or description.
     * @param took The time taken for the operation, in milliseconds.
     */
    public ElasticResponse(String errors, int took) {
        this.errors = errors;
    }

    public String getErrors() {
        return errors;
    }
}