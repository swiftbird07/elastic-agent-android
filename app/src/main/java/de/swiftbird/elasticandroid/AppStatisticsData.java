package de.swiftbird.elasticandroid;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity
public class AppStatisticsData {
    @PrimaryKey
    @ColumnInfo(name = "id")
    public int id;

    @ColumnInfo(name = "total_checkins")
    public int totalCheckins;

    @ColumnInfo(name = "total_failures")
    public int totalFailures;

    @ColumnInfo(name = "last_documents_sent_at")
    public String lastDocumentsSentAt;

    @ColumnInfo(name = "last_documents_sent_count")
    public int lastDocumentsSentCount;

    @ColumnInfo(name = "combined_buffer_size")
    public int combinedBufferSize;

    @ColumnInfo(name = "agent_health")
    public String agentHealth;


    public AppStatisticsData() {
        id = 1;
    }

}
