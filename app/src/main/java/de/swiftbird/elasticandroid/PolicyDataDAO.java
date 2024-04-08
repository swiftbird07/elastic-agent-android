package de.swiftbird.elasticandroid;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;


/**
 * Data Access Object (DAO) for managing {@link PolicyData} in the application's database.
 * Provides methods to insert, query, update, and delete policy data, allowing for real-time
 * and synchronized management of operational policies received from the Fleet server.
 *
 * <p>Includes specialized methods for handling backoff strategies to manage network communication
 * retries efficiently, ensuring the Elastic Agent remains resilient under various network conditions
 * and server response scenarios.</p>
 *
 * <p>Methods are annotated with Room annotations to define SQL queries and operations on the database.</p>
 *
 */
@Dao
public interface PolicyDataDAO {

    /**
     * Inserts a new {@link PolicyData} record into the database or replaces an existing one
     * if a conflict occurs based on the primary key.
     *
     * @param policyData The {@link PolicyData} object to insert.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertPolicyData(PolicyData policyData);

    /**
     * Queries the database asynchronously for the {@link PolicyData} record.
     *
     * @return A LiveData object containing the {@link PolicyData} record.
     */
    @Query("SELECT * FROM PolicyData")
    LiveData<PolicyData> getPoliyData();

    /**
     * Queries the database synchronously for the {@link PolicyData} record.
     *
     * @return The {@link PolicyData} record if found; null otherwise.
     */
    @Query("SELECT * FROM PolicyData")
    PolicyData getPolicyDataSync();


    /**
     * Updates the last updated timestamp and checkin action ID in the {@link PolicyData} record.
     *
     * @param lastUpdated The new last updated timestamp.
     * @param actionId The new checkin action ID.
     */
    @Query("UPDATE PolicyData SET last_updated = :lastUpdated, checkin_action_id = :actionId")
    void refreshPolicyData(String lastUpdated, String actionId);

    /**
     * Deletes all {@link PolicyData} records from the database.
     */
    @Query("DELETE FROM PolicyData")
    void delete();

    /**
     * Increases the backoff put interval (for communicating with Elasticsearch) by doubling the current value.
     */
    @Query("UPDATE PolicyData SET backoff_put_interval = backoff_put_interval * 2")
    void increaseBackoffPutInterval();

    /**
     * Sets the backoff put interval (for communicating with Elasticsearch) to a specific value.
     *
     * @param interval The new backoff put interval value.
     */
    @Query("UPDATE PolicyData SET backoff_put_interval = :interval")
    void setBackoffPutInterval(int interval);

    /**
     * Resets the backoff put (for communicating with Elasticsearch) interval to the default put interval value.
     */
    @Query("UPDATE PolicyData SET backoff_put_interval = put_interval")
    void resetBackoffPutInterval();

    /**
     * Increases the backoff checkin interval (for communicating with the Fleet server) by doubling the current value.
     */
    @Query("UPDATE PolicyData SET backoff_checkin_interval = backoff_checkin_interval * 2")
    void increaseBackoffCheckinInterval();

    /**
     * Sets the backoff checkin interval (for communicating with the Fleet server) to a specific value.
     *
     * @param interval The new backoff checkin interval value.
     */
    @Query("UPDATE PolicyData SET backoff_checkin_interval = :interval")
    void setBackoffCheckinInterval(int interval);

    /**
     * Resets the backoff checkin interval (for communicating with the Fleet server) to the default checkin interval value.
     */
    @Query("UPDATE PolicyData SET backoff_checkin_interval = checkin_interval")
    void resetBackoffCheckinInterval();
}
