package de.swiftbird.elasticandroid;

public class AppEnrollRequest {
    private String serverUrl;
    private String token;
    private String hostname;
    private String certificate;
    private boolean checkCert;
    private boolean fingerprintRootCA;

    protected AppEnrollRequest(String serverUrl, String token, String hostname, String certificate, boolean checkCert, boolean fingerprintRootCA){
        this.serverUrl = serverUrl;
        this.token = token;
        this.hostname = hostname;
        this.certificate = certificate;
        this.checkCert = checkCert;
        this.fingerprintRootCA = fingerprintRootCA;
    }

    protected String getServerUrl() {
        return serverUrl;
    }


    public String getHostname() {
        return hostname;
    }

    public String getCertificate() {
        return certificate;
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