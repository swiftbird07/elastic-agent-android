package de.swiftbird.elasticandroid;

import com.google.gson.annotations.SerializedName;

public class ElasticResponse {
    private String errors;
    private int took;

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

}