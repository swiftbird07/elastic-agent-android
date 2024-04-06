package de.swiftbird.elasticandroid;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

/**
 * Data Access Object (DAO) for managing operations related to the AppStatisticsData entity.
 * Defines methods for inserting, updating, and querying application statistics data from the database.
 */
@Dao
public interface AppStatisticsDataDAO {
    /**
     * Updates or replaces the existing statistics data in the database.
     *
     * @param statisticsData The statistics data to be updated or inserted.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void updateStatistics(AppStatisticsData statisticsData);

    /**
     * Increments the total number of check-ins by one.
     */
    @Query("UPDATE AppStatisticsData SET total_checkins = total_checkins + 1")
    void increaseTotalCheckins();

    /**
     * Increments the total number of failures by one.
     */
    @Query("UPDATE AppStatisticsData SET total_failures = total_failures + 1")
    void increaseTotalFailures();

    /**
     * Sets the timestamp for the last set of documents sent.
     *
     * @param lastDocumentsSentAt The timestamp when the last documents were sent.
     */
    @Query("UPDATE AppStatisticsData SET last_documents_sent_at = :lastDocumentsSentAt")
    void setLastDocumentsSentAt(String lastDocumentsSentAt);

    /**
     * Sets the count for the last set of documents sent.
     *
     * @param lastDocumentsSentCount The number of documents sent in the last operation.
     */
    @Query("UPDATE AppStatisticsData SET last_documents_sent_count = :lastDocumentsSentCount")
    void setLastDocumentsSentCount(int lastDocumentsSentCount);

    /**
     * Increases the combined buffer size by a specified amount.
     *
     * @param amount The amount by which to increase the combined buffer size.
     */
    @Query("UPDATE AppStatisticsData SET combined_buffer_size = combined_buffer_size + :amount")
    void increaseCombinedBufferSize(int amount);

    /**
     * Decreases the combined buffer size by a specified amount.
     *
     * @param amount The amount by which to decrease the combined buffer size.
     */
    @Query("UPDATE AppStatisticsData SET combined_buffer_size = combined_buffer_size - :amount")
    void decreaseCombinedBufferSize(int amount);

    /**
     * Updates the health status of the agent.
     *
     * @param agentHealth The new health status of the agent.
     */
    @Query("UPDATE AppStatisticsData SET agent_health = :agentHealth")
    void setAgentHealth(String agentHealth);

    /**
     * Inserts a new statistics data record into the database.
     * If the record already exists, it ignores the insertion.
     *
     * @param statisticsData The new statistics data to insert.
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insert(AppStatisticsData statisticsData);

    /**
     * Retrieves the application statistics data as a LiveData object.
     * LiveData allows observing data changes in a lifecycle-aware manner.
     *
     * @return A LiveData object containing the application statistics data.
     */
    @Query("SELECT * FROM AppStatisticsData WHERE id = 1")
    LiveData<AppStatisticsData> getStatistics();

    /**
     * Synchronous version of getStatistics for immediate data retrieval.
     *
     * @return The application statistics data.
     */
    @Query("SELECT * FROM AppStatisticsData WHERE id = 1")
    AppStatisticsData getStatisticsSync();

    /**
     * Deletes all statistics data records from the database.
     */
    @Query("DELETE FROM AppStatisticsData")
    void delete();
}
