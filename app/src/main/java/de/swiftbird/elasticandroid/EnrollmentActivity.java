package de.swiftbird.elasticandroid;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONObject;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.MessageFormat;

import de.swiftbird.elasticandroid.R.id;
import kotlin.NotImplementedError;

public class EnrollmentActivity extends AppCompatActivity implements StatusCallback {
    private static final String TAG = "EnrollmentActivity";
    private EditText etServerUrl, etToken, etHostname, etTags;
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
        etTags = findViewById(R.id.etTags);
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

        // Check build config and enable button if set
        boolean isBuildConfigSet = BuildConfig.ENROLLMENT_STRING != null && !BuildConfig.ENROLLMENT_STRING.isEmpty();
        btnLoadFromConfig.setEnabled(isBuildConfigSet);

        btnEnrollNow.setOnClickListener(view -> attemptEnrollment());
        btnLoadFromClipboard.setOnClickListener(v -> loadFromClipboard());
        btnLoadFromQR.setOnClickListener(v -> loadFromQRCode());
        btnLoadFromConfig.setOnClickListener(v -> loadFromBuildConfig());
    }

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

    private void loadFromQRCode() {
        // Trigger QR code scanning and handle result
        throw new NotImplementedError("QR code scanning is not implemented yet.");
        //String base64EnrollmentString = "exampleBase64String"; //TODO:  Replace with actual QR handling
        //autofillFromEnrollmentString(base64EnrollmentString);
    }

    private void loadFromBuildConfig() {
        String base64EnrollmentString = BuildConfig.ENROLLMENT_STRING;
        autofillFromEnrollmentString(base64EnrollmentString);
    }

    private void attemptEnrollment() {
        tError.setText("");
        String serverUrl = etServerUrl.getText().toString().trim();
        String token = etToken.getText().toString().trim();
        String hostname = etHostname.getText().toString().trim();
        String certificate = etTags.getText().toString().trim();
        boolean checkCA = swCheckCA.isChecked();
        boolean pinRootCA = swPinRootCA.isChecked();

        // Disable button to prevent multiple enrollment attempts
        btnEnrollNow.setEnabled(false);

        // Validate serverUrl
        try {
            new URL(serverUrl).toURI();
        } catch (URISyntaxException | MalformedURLException e) {
            Log.w(TAG, "Invalid server URL: " + serverUrl);
            Toast.makeText(this, "Invalid server URL. Please correct it.", Toast.LENGTH_LONG).show();
            btnEnrollNow.setEnabled(true);
            return;
        }

        // Define a pattern that allows letters, digits, underscore, hyphen, and period for token and hostname
        String base64Pattern = "^[A-Za-z0-9_\\-\\.=]+$";

        // Validate token
        if (!token.matches(base64Pattern)) {
            Log.w(TAG, "Invalid characters in token.");
            Toast.makeText(this, "Token contains invalid characters. Only letters, digits, '_', '-', '=' and '.' are allowed.", Toast.LENGTH_LONG).show();
            btnEnrollNow.setEnabled(true);
            return;
        }

        // Validate hostname
        if (!hostname.matches(base64Pattern)) {
            Log.w(TAG, "Invalid characters in hostname.");
            Toast.makeText(this, "Hostname contains invalid characters. Only letters, digits, '_', '-', and '.' are allowed.", Toast.LENGTH_LONG).show();
            btnEnrollNow.setEnabled(true);
            return;
        }

        // Match default encoded certificate pattern (---BEGIN CERTIFICATE--- to ---END CERTIFICATE---)
        String certificatePattern = "-----BEGIN CERTIFICATE-----.+-----END CERTIFICATE-----";

        // Validate certificate
        if (!certificate.matches(certificatePattern) && !certificate.isEmpty()) {
            Log.w(TAG, "Invalid characters in certificate.");
            Toast.makeText(this, "Certificate contains invalid characters. Please provide a valid certificate.", Toast.LENGTH_LONG).show();
            btnEnrollNow.setEnabled(true);
            return;
        }



        if (!serverUrl.isEmpty() && !token.isEmpty() && !hostname.isEmpty()) {

            tStatus.setText("Starting enrollment process...");

            AppEnrollRequest request = new AppEnrollRequest(serverUrl, token, hostname, certificate, checkCA, pinRootCA);
            EnrollmentRepository repository = new EnrollmentRepository(getApplicationContext(), request.getServerUrl(), request.getToken(), request.getCheckCert(),  tStatus, tError);
            repository.enrollAgent(request, this); // will callback onCallback

        } else {
            // Handle validation failure
            Toast.makeText(this, "Please fill in all fields (except certificate).", Toast.LENGTH_SHORT).show();
        }
    }

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
            etTags.setText(jsonObj.optString("certificate", ""));
            swCheckCA.setChecked(jsonObj.optBoolean("verifyServerCert", true));
            swPinRootCA.setChecked(jsonObj.optBoolean("pinRootCert", false));
            tError.setText("");
        } catch (Exception e) {
            tError.setText(MessageFormat.format("Failed to autofill: {0}", e.getMessage()));
        }
    }

    @Override
    public void onCallback(boolean success) {
        if (success) {
            Log.i(TAG, "Enrollment successful. Going back to main activity.");
            // Wait for the user to see the success message
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Log.w(TAG, "Thread sleep interrupted: " + e.getMessage());
            }
            finish();
        } else {
            Log.w(TAG, "Enrollment failed. Check logs for details.");
            // Delete enrollment data from database (as it is invalid)
            AppDatabase db = AppDatabase.getDatabase(this.getApplicationContext(), "enrollment-data");
            AppDatabase.databaseWriteExecutor.execute(() -> {
                db.enrollmentDataDAO().delete();
            });
            btnEnrollNow.setEnabled(true);

        }
    }
}
