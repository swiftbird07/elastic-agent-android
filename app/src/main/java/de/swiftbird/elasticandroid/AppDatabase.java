package de.swiftbird.elasticandroid;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Central abstract database class for the application, using the Room persistence library to manage SQLite database operations.
 * This class defines all entities that form the database and their respective DAOs for data access. It implements a singleton
 * pattern to ensure that only one instance of the database is created throughout the application's lifecycle.
 *
 * @Database annotation specifies the entities contained within the database and its version. The exportSchema attribute
 * enables schema export, which is useful for version tracking and migration. autoMigrations defines automatic migration paths
 * between database versions, though this is commented out in the provided code snippet.
 *
 * @TypeConverters annotation lists classes that provide custom conversions between database types and Java data types, enabling
 * storage of complex types in the database.
 */
@Database(
        entities = {
                FleetEnrollData.class,
                PolicyData.class,
                SelfLogCompDocument.class,
                AppStatisticsData.class,
                SecurityLogsCompDocument.class,
                NetworkLogsCompDocument.class,
                LocationCompDocument.class,
        },
        version = 31,
        exportSchema = true,
        autoMigrations = {
                //@AutoMigration(from = 30, to = 31),
        }
)
@TypeConverters({AppConverters.class})
public abstract class AppDatabase extends RoomDatabase {

    public abstract FleetEnrollDataDAO enrollmentDataDAO();
    public abstract PolicyDataDAO policyDataDAO();
    public abstract AppStatisticsDataDAO statisticsDataDAO();
    public  abstract  SelfLogCompBuffer selfLogCompBuffer();
    public abstract SecurityLogsCompBuffer securityLogCompBuffer();
    public abstract NetworkLogsCompBuffer networkLogsCompBuffer();
    public abstract LocationCompBuffer locationCompBuffer();

    private static final boolean FALLBACK_TO_DESTRUCTIVE_MIGRATION = BuildConfig.DEBUG; // Flag to enable destructive migration in debug mode
    private static final int NUMBER_OF_THREADS = 4; // Thread pool size for database write operations
    static final ExecutorService databaseWriteExecutor =
            Executors.newFixedThreadPool(NUMBER_OF_THREADS); // Executor service for asynchronous database operations
    private static volatile AppDatabase appDatabase; // Singleton instance of the database



    /**
     * Gets the singleton instance of the AppDatabase.
     * This method uses a double-checked locking pattern to initialize the AppDatabase instance in a thread-safe manner.
     * Debug builds opt for a destructive migration strategy for simplicity, while release builds use the default migration strategy.
     *
     * @param context The context used to build the database instance.
     * @return The singleton instance of AppDatabase.
     */
    protected static AppDatabase getDatabase(final Context context, String dbName) {
        if (appDatabase == null) {
            synchronized (AppDatabase.class) {
                if (appDatabase == null) {
                    Builder<AppDatabase> builder = Room.databaseBuilder(context.getApplicationContext(),
                            AppDatabase.class, "agent-data");

                    if(FALLBACK_TO_DESTRUCTIVE_MIGRATION){
                        builder.fallbackToDestructiveMigration();
                    }

                    appDatabase = builder.build();
                }
            }
        }
        return appDatabase;
    }
}