package de.swiftbird.elasticandroid;

import com.google.gson.annotations.SerializedName;

public class ElasticResponse {

    /*
    {
  "error": {
    "root_cause": [
      {
        "type": "parse_exception",
        "reason": "request body is required"
      }
    ],
    "type": "parse_exception",
    "reason": "request body is required"
  },
  "status": 400
}
     */
    private String errors;
    private int took;

    private Error error;

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



    // Constructor (mostly for testing)
    public ElasticResponse(String errors, int took) {
        this.errors = errors;
        this.took = took;
    }

    public String getErrors() {
        return errors;
    }

    public int getTook() {
        return took;
    }


    public Error getError() {
        return error;
    }

}