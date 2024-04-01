package de.swiftbird.elasticandroid;


import android.content.Context;

import java.util.Collections;
import java.util.List;

public class SelfLogComp implements Component {

    private static SelfLogComp selfLogComp;
    private SelfLogCompBuffer buffer;

    public static SelfLogComp getInstance() {
        // Singleton pattern
        if (selfLogComp == null) {
            selfLogComp = new SelfLogComp();
        }
        return selfLogComp;
    }

    @Override
    public void setup(Context context, FleetEnrollData enrollmentData, PolicyData policyData) {
        // Initialize Room database and get the DAO
        AppDatabase db = AppDatabase.getDatabase(context, "");
        buffer = db.selfLogCompBuffer();
    }

    @Override
    public void collectEvents(FleetEnrollData enrollmentData, PolicyData policyData) {
        return; // No-op for this component (logs are collected in real-time)
    }

    @Override
    public void addDocumentToBuffer(ElasticDocument document) {
        if (document instanceof SelfLogCompDocument && buffer != null) {
            buffer.insertDocument((SelfLogCompDocument) document);
        }
        else {
            return; // Ignore invalid documents
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
}
