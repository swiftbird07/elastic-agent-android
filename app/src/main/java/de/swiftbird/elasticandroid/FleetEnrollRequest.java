package de.swiftbird.elasticandroid;

import java.util.List;

public class FleetEnrollRequest {
    private String type;
    private Metadata metadata;
    private String enrollment_id;
    private String shared_id;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Metadata getMetadata() {
        return metadata;
    }

    public void setMetadata(Metadata metadata) {
        this.metadata = metadata;
    }

    public String getEnrollment_id() {
        return enrollment_id;
    }

    public void setEnrollment_id(String enrollment_id) {
        this.enrollment_id = enrollment_id;
    }

    public String getShared_id() {
        return shared_id;
    }

    public void setShared_id(String shared_id) {
        this.shared_id = shared_id;
    }


    public static class Metadata {
        private String user_provided; // Assuming JSON String
        private String local; // Assuming JSON String
        private List<String> tags;

        public String getUser_provided() {
            return user_provided;
        }

        public void setUser_provided(String user_provided) {
            this.user_provided = user_provided;
        }

        public String getLocal() {
            return local;
        }

        public void setLocal(String local) {
            this.local = local;
        }

        public List<String> getTags() {
            return tags;
        }

        public void setTags(List<String> tags) {
            this.tags = tags;
        }

        // Constructor, getters, and setters
    }
}
