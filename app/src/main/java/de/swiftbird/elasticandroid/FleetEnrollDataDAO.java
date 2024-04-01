package de.swiftbird.elasticandroid;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;


@Dao
public interface FleetEnrollDataDAO {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertEnrollmentInfo(FleetEnrollData enrollmentData);

    @Query("SELECT * FROM FleetEnrollData WHERE id = :id")
    LiveData<FleetEnrollData> getEnrollmentInfo(int id);

    @Query("SELECT * FROM FleetEnrollData WHERE id = :id")
    FleetEnrollData getEnrollmentInfoSync(int id);

    @Query("DELETE FROM FleetEnrollData")
    void delete();

}
