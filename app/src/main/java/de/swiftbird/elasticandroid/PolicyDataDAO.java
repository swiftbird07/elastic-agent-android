package de.swiftbird.elasticandroid;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.lifecycle.LiveData;


@Dao
public interface PolicyDataDAO {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertPolicyData(PolicyData policyData);

    @Query("SELECT * FROM PolicyData")
    LiveData<PolicyData> getPoliyData();

    @Query("SELECT * FROM PolicyData")
    PolicyData getPolicyDataSync(); // This method is synchronous

    // Update last_updated field
    @Query("UPDATE PolicyData SET last_updated = :lastUpdated")
    void updateLastUpdated(String lastUpdated);

    @Query("DELETE FROM PolicyData")
    void delete();

}
