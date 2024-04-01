package de.swiftbird.elasticandroid;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

@Dao
public interface AppStatisticsDataDAO {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void updateStatistics(AppStatisticsData statisticsData);

    // Update various statistics data

    // Increase the total check-ins count
    @Query("UPDATE AppStatisticsData SET total_checkins = total_checkins + 1")
    void increaseTotalCheckins();

    // Increase the total failures count
    @Query("UPDATE AppStatisticsData SET total_failures = total_failures + 1")
    void increaseTotalFailures();

    // Set the last documents sent at
    @Query("UPDATE AppStatisticsData SET last_documents_sent_at = :lastDocumentsSentAt")
    void setLastDocumentsSentAt(String lastDocumentsSentAt);

    // Set the last documents sent count
    @Query("UPDATE AppStatisticsData SET last_documents_sent_count = :lastDocumentsSentCount")
    void setLastDocumentsSentCount(int lastDocumentsSentCount);

    // Increase the combined buffer size by amount
    @Query("UPDATE AppStatisticsData SET combined_buffer_size = combined_buffer_size + :amount")
    void increaseCombinedBufferSize(int amount);

    // Decrease the combined buffer size by amount
    @Query("UPDATE AppStatisticsData SET combined_buffer_size = combined_buffer_size - :amount")
    void decreaseCombinedBufferSize(int amount);

    // Set the agent health
    @Query("UPDATE AppStatisticsData SET agent_health = :agentHealth")
    void setAgentHealth(String agentHealth);

    // Insert a new statistics data
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insert(AppStatisticsData statisticsData);

    @Query("SELECT * FROM AppStatisticsData WHERE id = 1")
    LiveData<AppStatisticsData> getStatistics();

    // Sync version of getStatistics
    @Query("SELECT * FROM AppStatisticsData WHERE id = 1")
    AppStatisticsData getStatisticsSync();

    @Query("DELETE FROM AppStatisticsData")
    void delete();
}
