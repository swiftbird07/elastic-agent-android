package de.swiftbird.elasticandroid;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.lifecycle.LiveData;


@Dao
public interface EnrollmentDataDAO {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertEnrollmentInfo(EnrollmentData enrollmentData);

    @Query("SELECT * FROM EnrollmentData WHERE id = :id")
    LiveData<EnrollmentData> getEnrollmentInfo(int id);

    @Query("SELECT * FROM EnrollmentData WHERE id = :id")
    EnrollmentData getEnrollmentInfoSync(int id);

    @Query("DELETE FROM EnrollmentData")
    void delete();

}
