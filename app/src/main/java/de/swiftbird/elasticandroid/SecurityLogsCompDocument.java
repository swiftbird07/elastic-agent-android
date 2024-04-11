package de.swiftbird.elasticandroid;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import com.google.gson.annotations.SerializedName;

/**
 * Represents a document for security log events within the Elastic Android application. This class extends the
 * {@link ElasticDocument} to include specific fields related to security events, such as log level, event tag, and the log message itself.
 * It's used to model and store information about security-related activities detected on the device, facilitating their later analysis
 * and processing.
 */

@Entity
public class SecurityLogsCompDocument extends ElasticDocument {
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    public int id;

    @SerializedName("event.action")
    @ColumnInfo(name = "event_action")
    public String eventAction = "security";

    @SerializedName("event.category")
    @ColumnInfo(name = "event_category")
    public String eventCategory = "log";

    @SerializedName("log.level")
    @ColumnInfo(name = "log_level")
    public String logLevel;

    @SerializedName("tag")
    @ColumnInfo(name = "tag")
    public String tag;

    @SerializedName("message")
    @ColumnInfo(name = "message")
    public String message;

    // Constructor using superclass constructor and setting own fields
    public SecurityLogsCompDocument() {}
    public SecurityLogsCompDocument(FleetEnrollData fleetEnrollData, PolicyData policyData, String logLevel, String tag, String message) {
        super(fleetEnrollData, policyData);
        this.logLevel = logLevel;
        this.tag = tag;
        this.message = message;
    }

}

