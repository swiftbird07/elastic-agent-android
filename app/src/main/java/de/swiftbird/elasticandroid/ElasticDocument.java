package de.swiftbird.elasticandroid;

import com.google.gson.annotations.SerializedName;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ElasticDocument {
    @SerializedName("@timestamp")
    private String timestamp;
    // Other common fields...

    public ElasticDocument() {
        this.timestamp = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).format(new Date());
    }

    // Getter and Setter for timestamp and other common fields...
}
