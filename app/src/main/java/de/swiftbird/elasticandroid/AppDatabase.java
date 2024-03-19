package de.swiftbird.elasticandroid;

import androidx.room.Database;
import androidx.room.RoomDatabase;

@Database(entities = {EnrollmentData.class}, version = 1)
public abstract class AppDatabase extends RoomDatabase {

    public abstract EnrollmentDataDAO EnrollmentDataDAO();
}