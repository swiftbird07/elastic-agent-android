package de.swiftbird.elasticandroid;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

/**
 * Data Access Object (DAO) for managing FleetEnrollData entities within the Room database.
 * Provides methods for inserting, querying, and deleting enrollment information of an Elastic Agent.
 */
@Dao
public interface FleetEnrollDataDAO {

    /**
     * Inserts a new FleetEnrollData record into the database or replaces an existing one
     * if a conflict occurs based on the primary key.
     *
     * @param enrollmentData The FleetEnrollData object to insert.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertEnrollmentInfo(FleetEnrollData enrollmentData);

    /**
     * Queries the database asynchronously for a FleetEnrollData record by its ID.
     *
     * @param id The ID of the enrollment record to find.
     * @return A LiveData object containing the FleetEnrollData record, if found.
     */
    @Query("SELECT * FROM FleetEnrollData WHERE id = :id")
    LiveData<FleetEnrollData> getEnrollmentInfo(int id);

    /**
     * Queries the database synchronously for a FleetEnrollData record by its ID.
     *
     * @param id The ID of the enrollment record to find.
     * @return The FleetEnrollData record if found; null otherwise.
     */
    @Query("SELECT * FROM FleetEnrollData WHERE id = :id")
    FleetEnrollData getEnrollmentInfoSync(int id);

    /**
     * Deletes all FleetEnrollData records from the database.
     */
    @Query("DELETE FROM FleetEnrollData")
    void delete();
}
