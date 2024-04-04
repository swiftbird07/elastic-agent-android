package de.swiftbird.elasticandroid;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import com.google.gson.annotations.SerializedName;

@Entity
public class SecurityLogsCompDocument extends ElasticDocument {
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    public int id;
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

