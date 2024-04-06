package de.swiftbird.elasticandroid;

/**
 * Represents a request to enroll an application with a server, containing all necessary information for the enrollment process.
 * This class encapsulates details such as the server URL, enrollment token, hostname, certificate, and security checks to be performed.
 */
public class AppEnrollRequest {
    private String serverUrl;
    private String token;
    private String hostname;
    private String certificate;
    private boolean checkCert;
    private boolean fingerprintRootCA;

    /**
     * Constructs an AppEnrollRequest with all necessary information for enrolling the app.
     *
     * @param serverUrl URL of the server.
     * @param token Enrollment token.
     * @param hostname Hostname of the device/system.
     * @param certificate Certificate information.
     * @param checkCert Whether to check the server's certificate.
     * @param fingerprintRootCA Whether to check for a fingerprint match with the root CA.
     */
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