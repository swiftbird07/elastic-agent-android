package de.swiftbird.elasticandroid;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;


@Dao
public interface PolicyDataDAO {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertPolicyData(PolicyData policyData);

    @Query("SELECT * FROM PolicyData")
    LiveData<PolicyData> getPoliyData();

    @Query("SELECT * FROM PolicyData")
    PolicyData getPolicyDataSync(); // This method is synchronous

    // Update last_updated field

    @Query("UPDATE PolicyData SET last_updated = :lastUpdated, checkin_action_id = :actionId")
    void refreshPolicyData(String lastUpdated, String actionId);


    @Query("DELETE FROM PolicyData")
    void delete();

    // Backoff logic
    @Query("UPDATE PolicyData SET backoff_put_interval = backoff_put_interval * 2")
    void increaseBackoffPutInterval();

    @Query("UPDATE PolicyData SET backoff_put_interval = :interval")
    void setBackoffPutInterval(int interval);

    @Query("UPDATE PolicyData SET backoff_put_interval = put_interval")
    void resetBackoffPutInterval();

    @Query("UPDATE PolicyData SET backoff_checkin_interval = backoff_checkin_interval * 2")
    void increaseBackoffCheckinInterval();

    @Query("UPDATE PolicyData SET backoff_checkin_interval = :interval")
    void setBackoffCheckinInterval(int interval);

    @Query("UPDATE PolicyData SET backoff_checkin_interval = checkin_interval")
    void resetBackoffCheckinInterval();
}
