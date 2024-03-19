package de.swiftbird.elasticandroid;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity
public class EnrollmentData {
    @PrimaryKey(autoGenerate = true)
    public int id;

    @ColumnInfo(name = "agent_id")
    public String agentId;

    @ColumnInfo(name = "server_url")
    public String serverUrl;
}
