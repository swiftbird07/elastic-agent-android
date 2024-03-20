package de.swiftbird.elasticandroid;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONObject;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.MessageFormat;
import java.util.Base64;

import de.swiftbird.elasticandroid.R.id;

public class EnrollmentActivity extends AppCompatActivity {
    private static final String TAG = "EnrollmentActivity";
    private EnrollmentViewModel viewModel;
    private EditText etServerUrl, etToken, etHostname, etTags;
    private androidx.appcompat.widget.SwitchCompat swCheckCA, swPinRootCA;
    private TextView tError;

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
        Button btnEnrollNow = findViewById(R.id.btnEnrollNow);
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
        String base64EnrollmentString = "exampleBase64String"; //TODO:  Replace with actual QR handling
        autofillFromEnrollmentString(base64EnrollmentString);
    }

    private void loadFromBuildConfig() {
        String base64EnrollmentString = BuildConfig.ENROLLMENT_STRING;
        autofillFromEnrollmentString(base64EnrollmentString);
    }

    private void attemptEnrollment() {
        String serverUrl = etServerUrl.getText().toString().trim();
        String token = etToken.getText().toString().trim();
        String hostname = etHostname.getText().toString().trim();
        String certificate = etTags.getText().toString().trim();
        boolean checkCA = swCheckCA.isChecked();
        boolean pinRootCA = swPinRootCA.isChecked();

        // Assuming this code is within an Activity and serverUrl, token, hostname, and certificate are already defined

        // Validate serverUrl
        try {
            new URL(serverUrl).toURI();
        } catch (URISyntaxException | MalformedURLException e) {
            Log.w(TAG, "Invalid server URL: " + serverUrl);
            Toast.makeText(this, "Invalid server URL. Please correct it.", Toast.LENGTH_LONG).show();
            return; // Exit the method or handle accordingly
        }

        // Define a pattern that allows letters, digits, underscore, hyphen, and period for token and hostname
        String base64Pattern = "^[A-Za-z0-9_\\-\\.]+$";

        // Validate token
        if (!token.matches(base64Pattern)) {
            Log.w(TAG, "Invalid characters in token.");
            Toast.makeText(this, "Token contains invalid characters. Only letters, digits, '_', '-', and '.' are allowed.", Toast.LENGTH_LONG).show();
            return; // Exit the method or handle accordingly
        }

        // Validate hostname
        if (!hostname.matches(base64Pattern)) {
            Log.w(TAG, "Invalid characters in hostname.");
            Toast.makeText(this, "Hostname contains invalid characters. Only letters, digits, '_', '-', and '.' are allowed.", Toast.LENGTH_LONG).show();
            return; // Exit the method or handle accordingly
        }

        // Match default encoded certificate pattern (---BEGIN CERTIFICATE--- to ---END CERTIFICATE---)
        String certificatePattern = "-----BEGIN CERTIFICATE-----.+-----END CERTIFICATE-----";

        // Validate certificate
        if (!certificate.matches(certificatePattern)) {
            Log.w(TAG, "Invalid characters in certificate.");
            Toast.makeText(this, "Certificate contains invalid characters. Please provide a valid certificate.", Toast.LENGTH_LONG).show();
            return; // Exit the method or handle accordingly
        }



        if (!serverUrl.isEmpty() && !token.isEmpty() && !hostname.isEmpty()) {

            EnrollmentRequest request = new EnrollmentRequest(serverUrl, token, hostname, certificate, checkCA, pinRootCA);
            viewModel = new EnrollmentViewModel(getApplication(), request, tError);

            // Trigger the enrollment and observe the result
            viewModel.enrollAgent(request).observe(this, enrollmentResponse -> {
                if (enrollmentResponse != null) {
                    // Handle success
                    Toast.makeText(this, "Enrollment Successful!", Toast.LENGTH_SHORT).show();
                    // Optionally, navigate away or update UI accordingly
                } else {
                    // Handle error
                    Toast.makeText(this, "Enrollment Failed!", Toast.LENGTH_SHORT).show();
                }
            });
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

}
