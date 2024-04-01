package de.swiftbird.elasticandroid;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Database(entities = {FleetEnrollData.class, PolicyData.class, SelfLogCompDocument.class}, version = 10)
public abstract class AppDatabase extends RoomDatabase {

    public abstract FleetEnrollDataDAO enrollmentDataDAO();

    public abstract PolicyDataDAO policyDataDAO();

    public  abstract  SelfLogCompBuffer selfLogCompBuffer();

    private static volatile AppDatabase INSTANCE;

    private static final int NUMBER_OF_THREADS = 4;
    static final ExecutorService databaseWriteExecutor =
            Executors.newFixedThreadPool(NUMBER_OF_THREADS);

    public static AppDatabase getDatabase(final Context context, String name) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    // Allow destructive migration to simplify the code. This is not recommended for production apps. TODO: Implement proper migration
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                            AppDatabase.class, "agent-data").fallbackToDestructiveMigration().build();
                }
            }
        }
        return INSTANCE;
    }
}