package de.swiftbird.elasticandroid;

public class EnrollmentRequest {
    private String serverUrl;
    private String token;
    private String hostname;
    private String tags;
    private boolean checkCert;
    private boolean fingerprintRootCA;

    protected EnrollmentRequest(String serverUrl, String token, String hostname, String tags, boolean checkCert, boolean fingerprintRootCA){
        this.serverUrl = serverUrl;
        this.token = token;
        this.hostname = hostname;
        this.tags = tags;
        this.checkCert = checkCert;
        this.fingerprintRootCA = fingerprintRootCA;
    }

    protected String getServerUrl() {
        return serverUrl;
    }


    public String getHostname() {
        return hostname;
    }

    public String getTags() {
        return tags;
    }

    public boolean getCheckCert() {
        return checkCert;
    }

    public void setCheckCert(boolean checkCert) {
        this.checkCert = checkCert;
    }

    public boolean isFingerprintRootCA() {
        return fingerprintRootCA;
    }

    public void setFingerprintRootCA(boolean fingerprintRootCA) {
        this.fingerprintRootCA = fingerprintRootCA;
    }

    protected String getToken() {
        return token;
    }
}
