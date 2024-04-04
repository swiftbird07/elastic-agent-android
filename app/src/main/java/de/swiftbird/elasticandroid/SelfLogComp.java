package de.swiftbird.elasticandroid;


import android.content.Context;
import android.util.Log;

import java.util.Collections;
import java.util.List;

public class SelfLogComp implements Component {

    private static SelfLogComp selfLogComp;
    private SelfLogCompBuffer buffer;
    private AppStatisticsDataDAO statistic;

    public static SelfLogComp getInstance() {
        // Singleton pattern
        if (selfLogComp == null) {
            selfLogComp = new SelfLogComp();
        }
        return selfLogComp;
    }

    @Override
    public boolean setup(Context context, FleetEnrollData enrollmentData, PolicyData policyData, String subComponent) {
        // Initialize Room database and get the DAO
        AppDatabase db = AppDatabase.getDatabase(context, "");
        buffer = db.selfLogCompBuffer();
        statistic = db.statisticsDataDAO();
        return true;
    }

    @Override
    public void collectEvents(FleetEnrollData enrollmentData, PolicyData policyData) {
        return; // No-op for this component (logs are collected in real-time)
    }

    @Override
    public void addDocumentToBuffer(ElasticDocument document) {
        if (document instanceof SelfLogCompDocument && buffer != null) {
            buffer.insertDocument((SelfLogCompDocument) document);
            statistic.increaseCombinedBufferSize(1);
        }
        else {
            Log.w("SelfLogComp", "Invalid document type or buffer not initialized");
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
    public List<String> getRequiredPermissions() {
        return Collections.emptyList();
    }

    @Override
    public String getPathName() {
        return "self-log";
    }

    @Override
    public void disable(Context context, FleetEnrollData enrollmentData, PolicyData policyData) {
        // No-op for this component
    }
}
