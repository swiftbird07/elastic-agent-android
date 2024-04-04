package de.swiftbird.elasticandroid;

import android.content.Context;

import java.util.List;

public interface Component { ;
    public boolean setup(Context context, FleetEnrollData enrollmentData, PolicyData policyData, String subComponent);
    public void collectEvents(FleetEnrollData enrollmentData, PolicyData policyData);
    void addDocumentToBuffer(ElasticDocument document);
    public <T extends ElasticDocument> List<T> getDocumentsFromBuffer(int maxDocuments);
    public int getDocumentsInBufferCount();
    public List<String> getRequiredPermissions();
    String getPathName();
    void disable(Context context, FleetEnrollData enrollmentData, PolicyData policyData);
}
