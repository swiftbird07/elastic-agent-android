package de.swiftbird.elasticandroid;

/**
 * Represents a request to enroll the app to the fleet server, containing all necessary information for the enrollment process.
 * This class encapsulates details such as the server URL, enrollment token, hostname, certificate, and security checks to be performed.
 * Don't confuse this class with the FleetEnrollRequest class, which is the final parsed request send used by retrofit (this is an intermediate representation).
 */
public class AppEnrollRequest {
    private final String serverUrl;
    private final String token;
    private final String hostname;
    private final String certificate;
    private final boolean checkCert;
    private final boolean fingerprintRootCA;

    /**
     * Constructs an AppEnrollRequest with all necessary information for enrolling the app.
     * Warning: Expects parameters to be sanitized and validated *before* calling this constructor.
     *
     * @param serverUrl URL of the fleet server.
     * @param token Enrollment token.
     * @param hostname Hostname of the device.
     * @param certificate Certificate of the fleet server in PEM format (UTF-8, Raw). Effective only if checkCert is true.
     *                    If empty, the server's certificate will be checked against the system's trust store.
     *                    Currently not implemented.
     * @param checkCert Whether to check the server's certificate (either against system trust store or the provided certificate if not empty).
     * @param fingerprintRootCA Whether to save the fingerprint of the fleet server's root CA certificate after successful enrollment and validate future connections against it.
     *                          Currently not implemented.
     */
    public AppEnrollRequest(String serverUrl, String token, String hostname, String certificate, boolean checkCert, boolean fingerprintRootCA){
        this.serverUrl = serverUrl;
        this.token = token;
        this.hostname = hostname;
        this.certificate = certificate;
        this.checkCert = checkCert;
        this.fingerprintRootCA = fingerprintRootCA;
    }

    public String getServerUrl() {
        return serverUrl;
    }


    public String getHostname() {
        return hostname;
    }

    public String getCertificate() {
        return certificate;
    }

    public boolean isCheckCert() {
        return checkCert;
    }

    public boolean isCheckFingerprintCert() {
        return fingerprintRootCA;
    }

    public String getToken() {
        return token;
    }
}