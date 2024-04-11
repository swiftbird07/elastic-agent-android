package de.swiftbird.elasticandroid;

import android.content.Context;
import android.util.Log;
import java.util.List;

/**
 * Manages the collection and storage of internal application logs for the Elastic Android application.
 * This component facilitates the recording of operational events, errors, and informational messages within the app,
 * helping in debugging and monitoring activities.
 *
 * <p>Logs are captured and stored persistently using a Room database, allowing for their retrieval and analysis over time.
 * The {@link SelfLogCompDocument} class represents individual log entries, which are managed by this component.</p>
 *
 * <p>The {@link AppLog} class is utilized to add logs to the buffer. It abstracts the complexity of directly interacting with
 * the logging mechanism, providing a simple interface for recording logs from anywhere within the application.</p>
 *
 * <p>For method documentation, refer to the Component interface.
 */
public class SelfLogComp implements Component {

    private static SelfLogComp selfLogComp;
    private SelfLogCompBuffer buffer;
    private AppStatisticsDataDAO statistic;

    public static synchronized SelfLogComp getInstance() {
        // Singleton pattern
        if (selfLogComp == null) {
            selfLogComp = new SelfLogComp();
        }
        return selfLogComp;
    }

    @Override
    public boolean setup(Context context, FleetEnrollData enrollmentData, PolicyData policyData, String subComponent) {
        // Initialize Room database and get the DAO
        AppDatabase db = AppDatabase.getDatabase(context);
        buffer = db.selfLogCompBuffer();
        statistic = db.statisticsDataDAO();
        return true;
    }

    @Override
    public void addDocumentToBuffer(ElasticDocument document) {
        if (document instanceof SelfLogCompDocument) {
            if (buffer != null) {
                buffer.insertDocument((SelfLogCompDocument) document);
                statistic.increaseCombinedBufferSize(1);
            } else {
                Log.e("SelfLogComp", "Buffer not initialized");
                throw new IllegalStateException("SelfLogComp buffer has not been initialized.");
            }
        } else {
            Log.e("SelfLogComp", "Invalid document type provided");
            throw new IllegalArgumentException("Only SelfLogCompDocument instances can be added to the buffer.");
        }
    }


    @Override
    public <T extends ElasticDocument> List<T> getDocumentsFromBuffer(int maxDocuments) {
        int toIndex = Math.min(maxDocuments, buffer.getDocumentCount());
        List<SelfLogCompDocument> logBuffer = buffer.getOldestDocuments(toIndex);
        buffer.deleteOldestDocuments(toIndex);

        @SuppressWarnings("unchecked") // Safe cast
        List<T> result = (List<T>) logBuffer;
        return result;
    }

    public int getDocumentsInBufferCount() {
        return buffer.getDocumentCount();
    }

    @Override
    public String getPathName() {
        return "self-log";
    }

    @Override
    public void disable(Context context, FleetEnrollData enrollmentData, PolicyData policyData) {
        try {
            AppLog.shutdownLogger();
            this.statistic = null;
            this.buffer = null;
        } catch (Exception e) {
            Log.e("SelfLogComp", "Failed to shut down logger properly", e);
        }
    }
}
