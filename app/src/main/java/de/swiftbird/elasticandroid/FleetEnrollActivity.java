package de.swiftbird.elasticandroid;

import android.annotation.SuppressLint;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import org.json.JSONObject;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.cert.CertificateException;
import java.text.MessageFormat;
import de.swiftbird.elasticandroid.R.id;
import kotlin.NotImplementedError;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.io.ByteArrayInputStream;

/**
 * Activity for handling the enrollment process of an Elastic Agent with a Fleet server.
 * Provides UI for inputting necessary enrollment information and initiates the enrollment process.
 * Supports loading enrollment data from the clipboard, a QR code, or build configuration.
 */
public class FleetEnrollActivity extends AppCompatActivity implements StatusCallback {
    private static final String TAG = "FleetEnrollActivity";
    private EditText etServerUrl, etToken, etHostname, etFleetCert;
    private androidx.appcompat.widget.SwitchCompat swCheckCA, swPinRootCA;
    private TextView tError, tStatus;
    private Button btnEnrollNow;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_enrollment);

        etServerUrl = findViewById(R.id.etServerUrl);
        etToken = findViewById(R.id.etToken);
        etHostname = findViewById(R.id.etHostname);
        etFleetCert = findViewById(R.id.etFleetCert);
        swCheckCA = findViewById(R.id.swCheckCA);
        swPinRootCA = findViewById(R.id.swPinRootCA);
        tError = findViewById(id.tError);
        tStatus = findViewById(id.tStatus);

        btnEnrollNow = findViewById(R.id.btnEnrollNow);
        Button btnLoadFromClipboard = findViewById(R.id.btnLoadFromClipboard);
        Button btnLoadFromConfig = findViewById(R.id.btnLoadFromConfig);
        Button btnLoadFromQR = findViewById(id.btnLoadFromQR);

        // Disable camera feature for now TODO
        btnLoadFromQR.setEnabled(false);

        // Disable cert pinning option for now TODO
        swPinRootCA.setEnabled(false);

        // Check build config and enable button if set
        @SuppressWarnings("ConstantConditions") // Always true/false at compile time, but changes depending on build variant
        boolean isBuildConfigSet = BuildConfig.ENROLLMENT_STRING != null && !BuildConfig.ENROLLMENT_STRING.isEmpty();
        btnLoadFromConfig.setEnabled(isBuildConfigSet);

        btnEnrollNow.setOnClickListener(view -> attemptEnrollment());
        btnLoadFromClipboard.setOnClickListener(v -> loadFromClipboard());
        btnLoadFromQR.setOnClickListener(v -> loadFromQRCode());
        btnLoadFromConfig.setOnClickListener(v -> loadFromBuildConfig());
    }

    /**
     * Loads the enrollment data from the clipboard and autofills the input fields.
     * Hint: The enrollment string can be generated using the create_enrollment_string.sh script.
     */
    private void loadFromClipboard() {
        CharSequence base64EnrollmentString = "";
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        boolean isClipboardEmpty = true;
        if (clipboard.hasPrimaryClip()) {
            ClipData clip = clipboard.getPrimaryClip();
            if (clip != null && clip.getItemCount() > 0) {
                base64EnrollmentString = clip.getItemAt(0).getText();
                isClipboardEmpty = (base64EnrollmentString == null || base64EnrollmentString.toString().isEmpty());
            }
        }
        if (!isClipboardEmpty) {
            autofillFromEnrollmentString(base64EnrollmentString.toString());
        }
        else{
            Toast.makeText(this, "Clipboard is empty", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Loads the enrollment data from a QR code and autofills the input fields.
     * Not implemented yet.
     * TODO: Implement QR code scanning.
     */
    private void loadFromQRCode() {
        // Trigger QR code scanning and handle result
        throw new NotImplementedError("QR code scanning is not implemented yet.");
        //String base64EnrollmentString = "exampleBase64String";
        //autofillFromEnrollmentString(base64EnrollmentString);
    }

    /**
     * Loads the enrollment data from the build configuration and autofills the input fields.
     * Useful for mass enrollment of devices with a pre-configured enrollment string.
     * The enrollment string should be stored in the BuildConfig.ENROLLMENT_STRING field.
     * Hint: The enrollment string can be generated using the create_enrollment_string.sh script.
     */
    private void loadFromBuildConfig() {
        String base64EnrollmentString = BuildConfig.ENROLLMENT_STRING;
        autofillFromEnrollmentString(base64EnrollmentString);
    }

    /**
     * Initiates the enrollment process by validating the input fields and sending an enrollment request to the Fleet server.
     */
    @SuppressLint("SetTextI18n") // Suppress warning for hardcoded text (App is English-only anyway)
    private void attemptEnrollment() {
        tError.setText("");
        String serverUrl = etServerUrl.getText().toString().trim();
        String token = etToken.getText().toString().trim();
        String hostname = etHostname.getText().toString().trim();
        String certificate = etFleetCert.getText().toString().trim();
        boolean checkCA = swCheckCA.isChecked();
        boolean pinRootCA = swPinRootCA.isChecked();

        // Disable button to prevent multiple enrollment attempts
        btnEnrollNow.setEnabled(false);

        // Validate serverUrl
        try {
            new URL(serverUrl).toURI();
        } catch (URISyntaxException | MalformedURLException e) {
            AppLog.w(TAG, "Invalid server URL: " + serverUrl);
            Toast.makeText(this, "Invalid server URL. Please correct it.", Toast.LENGTH_LONG).show();
            btnEnrollNow.setEnabled(true);
            return;
        }

        // Define a pattern that allows letters, digits, underscore, hyphen, and period for token and hostname
        @SuppressWarnings("RegExpRedundantEscape") // Not redundant, removing escape will error "Unclosed character class"
        String base64Pattern = "^[A-Za-z0-9_\\-\\.=]+$";

        // Validate token
        if (!token.matches(base64Pattern)) {
            AppLog.w(TAG, "Invalid characters in token.");
            Toast.makeText(this, "Token contains invalid characters. Only letters, digits, '_', '-', '=' and '.' are allowed.", Toast.LENGTH_LONG).show();
            btnEnrollNow.setEnabled(true);
            return;
        }

        // Validate hostname
        if (!hostname.matches(base64Pattern)) {
            AppLog.w(TAG, "Invalid characters in hostname.");
            Toast.makeText(this, "Hostname contains invalid characters. Only letters, digits, '_', '-', and '.' are allowed.", Toast.LENGTH_LONG).show();
            btnEnrollNow.setEnabled(true);
            return;
        }

        // Validate certificate
        if (!certificate.isEmpty() && !validatePEMCertString(certificate)) {
            AppLog.w(TAG, "Certificate is invalid.");
            Toast.makeText(this, "Certificate could not be parsed. Please check the format.", Toast.LENGTH_LONG).show();
            btnEnrollNow.setEnabled(true);
            return;
        }

        if (!serverUrl.isEmpty() && !token.isEmpty() && !hostname.isEmpty()) {
            tStatus.setText("Starting enrollment process...");
            AppEnrollRequest request = new AppEnrollRequest(serverUrl, token, hostname, certificate, checkCA, pinRootCA);
            FleetEnrollRepository repository = new FleetEnrollRepository(getApplicationContext(), request.getServerUrl(), request.getToken(), request.getCertificate(), request.isCheckCert(),  tStatus, tError);
            repository.enrollAgent(request, this); // will callback onCallback

        } else {
            // Handle validation failure
            Toast.makeText(this, "Please fill in all fields (except certificate).", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Autofills the input fields with the provided enrollment string (clipboard, QR code, or build configuration).
     * @param base64EnrollmentString The base64-encoded enrollment data to autofill the input fields.
     */
    private void autofillFromEnrollmentString(String base64EnrollmentString) {
        try {
            String jsonString = new String(android.util.Base64.decode(base64EnrollmentString, android.util.Base64.DEFAULT));
            JSONObject jsonObj = new JSONObject(jsonString);

            // Attempt to replace %DEVICENAME% placeholder with the device's model name
            String hostname = jsonObj.optString("hostname", "");
            hostname = hostname.replace("%DEVICENAME%", Build.ID); // Using Build.MODEL as the device name

            etServerUrl.setText(jsonObj.optString("serverUrl", ""));
            etToken.setText(jsonObj.optString("token", ""));
            etHostname.setText(hostname); // Set the potentially modified hostname
            etFleetCert.setText(jsonObj.optString("certificate", ""));
            swCheckCA.setChecked(jsonObj.optBoolean("verifyServerCert", true));
            swPinRootCA.setChecked(jsonObj.optBoolean("pinRootCert", false));
            tError.setText("");
        } catch (Exception e) {
            tError.setText(MessageFormat.format("Failed to autofill: {0}", e.getMessage()));
        }
    }

    /**
     * Callback method for handling the enrollment result.
     * @param success True if the enrollment was successful, false otherwise.
     */
    @Override
    public void onCallback(boolean success) {
        if (success) {
            AppLog.i(TAG, "Enrollment successful. Going back to main activity.");

            // Wait for the user to see the success message
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ignored) { // Ignore the exception, as it is not critical
            }
            finish();
        } else {
            AppLog.w(TAG, "Enrollment failed. Check logs for details.");

            // Delete enrollment data from database (as it is invalid)
            AppDatabase db = AppDatabase.getDatabase(this.getApplicationContext(), "enrollment-data");
            AppDatabase.databaseWriteExecutor.execute(() -> db.enrollmentDataDAO().delete());
            AppDatabase.databaseWriteExecutor.execute(() -> db.policyDataDAO().delete());
            AppDatabase.databaseWriteExecutor.execute(() -> db.selfLogCompBuffer().deleteAllDocuments());
            AppDatabase.databaseWriteExecutor.execute(() -> db.statisticsDataDAO().delete());

            // Remove all registered workers
            WorkScheduler.cancelAllWork(getApplicationContext());

            btnEnrollNow.setEnabled(true);

        }
    }


    private static boolean validatePEMCertString(String certificate)
    {
        String BEGIN_CERT = "-----BEGIN CERTIFICATE-----";
        String END_CERT = "-----END CERTIFICATE-----";

        try {
            // Check for BEGIN/END markers
            if (!certificate.contains(BEGIN_CERT) || !certificate.contains(END_CERT)) {
                AppLog.w(TAG, "Certificate is missing BEGIN/END markers.");
                return false;
            }

            // Extract base64 encoded part by removing possible headers and footers
            String encodedCert = certificate
                    .replaceAll(BEGIN_CERT, "")
                    .replaceAll(END_CERT, "")
                    .replaceAll("\\s", ""); // Remove whitespace

            // Decode the base64 encoded certificate
            byte[] decodedBytes = android.util.Base64.decode(encodedCert, android.util.Base64.DEFAULT);

            // Generate certificate object
            CertificateFactory certFactory = CertificateFactory.getInstance("X.509");
            X509Certificate cert = (X509Certificate) certFactory.generateCertificate(new ByteArrayInputStream(decodedBytes));

            // Check certificate validity
            cert.checkValidity(); // This will throw a CertificateExpiredException or CertificateNotYetValidException if not valid
            AppLog.d(TAG, "Certificate is valid: " + cert.getSubjectDN().getName());
            return true;

        } catch (IllegalArgumentException | CertificateException e) {
            AppLog.w(TAG, "Invalid certificate: " + e.getMessage());
            return false;
        }
    }

}
