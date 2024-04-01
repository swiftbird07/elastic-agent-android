package de.swiftbird.elasticandroid;

import android.content.Context;

import java.util.List;

public interface Component { ;
    public void setup(Context context, FleetEnrollData enrollmentData, PolicyData policyData);
    public void collectEvents(FleetEnrollData enrollmentData, PolicyData policyData);
    void addDocumentToBuffer(ElasticDocument document);
    public <T extends ElasticDocument> List<T> getDocumentsFromBuffer(int maxDocuments);
    public int getDocumentsInBufferCount();
    public List<String> getRequiredPermissions();
}
