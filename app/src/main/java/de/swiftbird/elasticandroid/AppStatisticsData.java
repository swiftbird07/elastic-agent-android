package de.swiftbird.elasticandroid;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.OnConflictStrategy;
import androidx.room.PrimaryKey;

/**
 * Entity representing various statistical data points collected about the application's performance and activities.
 * This includes metrics such as the total number of check-ins, failures, details about the last documents sent,
 * the combined buffer size for pending operations, and the overall health status of the agent.
 */
@Entity
public class AppStatisticsData {
    @PrimaryKey
    @ColumnInfo(name = "id")
    public int id; // Unique identifier for the statistics data record. Currently set to 1 as a singleton pattern.

    @ColumnInfo(name = "total_checkins")
    public int totalCheckins; // The total number of check-ins performed by the application.

    @ColumnInfo(name = "total_failures")
    public int totalFailures; // The total number of failures encountered by the application.

    @ColumnInfo(name = "last_documents_sent_at")
    public String lastDocumentsSentAt; // Timestamp of the last successful document transmission.

    @ColumnInfo(name = "last_documents_sent_count")
    public int lastDocumentsSentCount; // Number of documents sent in the last transmission.

    @ColumnInfo(name = "combined_buffer_size")
    public int combinedBufferSize; // The combined size of all buffers awaiting transmission.

    @ColumnInfo(name = "agent_health")
    public String agentHealth; // Descriptive status of the agent's health.

    /**
     * Constructor initializing the statistics data with a default id.
     * This ensures that the entity acts as a singleton, only allowing a single set of statistics data.
     */
    public AppStatisticsData() {
        id = 1; // Ensures singleton behavior by using a constant ID.
    }
}
