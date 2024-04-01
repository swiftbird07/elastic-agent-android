package de.swiftbird.elasticandroid;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import com.google.gson.annotations.SerializedName;

@Entity
public class SelfLogCompDocument extends ElasticDocument {
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    public int id;
    @SerializedName("logLevel")
    @ColumnInfo(name = "log_level")
    public String logLevel;

    @SerializedName("tag")
    @ColumnInfo(name = "tag")
    public String tag;

    @SerializedName("message")
    @ColumnInfo(name = "message")
    public String message;

    @SerializedName("@timestamp")
    @ColumnInfo(name = "timestamp")
    public String timestamp;
}

