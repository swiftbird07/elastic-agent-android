package de.swiftbird.elasticandroid;

public class ApiResponse {
    private ApiResponse.Data data;

    // Getter and setter
    public ApiResponse.Data getData() {
        return data;
    }

    public void setData(ApiResponse.Data data) {
        this.data = data;
    }

    public static class Data {
        private String api_version;
        private String revision;
        private String hostname;


        public void setHostname(String hostname) {
            this.hostname = hostname;
        }

        public String getHostname() {
            return hostname;
        }

        public String getRevision() {
            return revision;
        }

        public void setRevision(String revision) {
            this.revision = revision;
        }

        public String getApi_version() {
            return api_version;
        }

        public void setApi_version(String api_version) {
            this.api_version = api_version;
        }
    }
}
