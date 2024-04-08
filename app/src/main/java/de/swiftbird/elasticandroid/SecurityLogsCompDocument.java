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
 *
 * <p>The document captures various details about each security event, including:</p>
 * <ul>
 *     <li>{@code eventAction} - A generic action identifier for security events, typically set to "security" to denote the nature of the log.</li>
 *     <li>{@code eventCategory} - Categorizes the log as a "log" event, distinguishing it from other event types within the system.</li>
 *     <li>{@code logLevel} - Indicates the severity or importance of the log, such as ERROR, WARNING, INFO, etc.</li>
 *     <li>{@code tag} - Provides a specific identifier or name for the event, often corresponding to a particular type of security event.</li>
 *     <li>{@code message} - Contains the detailed message or information about the event, which may include specifics about the security incident or observation.</li>
 * </ul>
 *
 * <p>This document structure supports the comprehensive logging and analysis of security-related events, contributing to the overall
 * security management and monitoring capabilities of the application. It allows for the persistent storage of security logs,
 * ensuring that critical security information is retained for review and response.</p>
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

