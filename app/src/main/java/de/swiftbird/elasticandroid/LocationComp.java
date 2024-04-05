package de.swiftbird.elasticandroid;

import android.content.Context;

import java.util.List;

public class LocationComp implements Component {
    @Override
    public boolean setup(Context context, FleetEnrollData enrollmentData, PolicyData policyData, String subComponent) {
        return false;
    }

    @Override
    public void collectEvents(FleetEnrollData enrollmentData, PolicyData policyData) {

    }

    @Override
    public void addDocumentToBuffer(ElasticDocument document) {

    }

    @Override
    public <T extends ElasticDocument> List<T> getDocumentsFromBuffer(int maxDocuments) {
        return null;
    }

    @Override
    public int getDocumentsInBufferCount() {
        return 0;
    }

    @Override
    public List<String> getRequiredPermissions() {
        return null;
    }

    @Override
    public String getPathName() {
        return null;
    }

    @Override
    public void disable(Context context, FleetEnrollData enrollmentData, PolicyData policyData) {

    }
}
