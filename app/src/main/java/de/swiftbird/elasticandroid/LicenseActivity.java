package de.swiftbird.elasticandroid;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.gms.oss.licenses.OssLicensesMenuActivity;

/**
 * LicenseActivity displays the license information for the application.
 * It provides information about the software's license, warranty, and liability.
 * It also provides a button to view the open source licenses.
 */
public class LicenseActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_licenses);
        Button btnBack = findViewById(R.id.btnBack);
        Button btnOSSLicenses = findViewById(R.id.btnOSSLicenses);
        TextView tvLicenseText = findViewById(R.id.tvLicenseText);
        String mitLicenseText = getString(R.string.mit_license_text);

        tvLicenseText.setText(mitLicenseText);

        btnOSSLicenses.setOnClickListener(view -> {
            OssLicensesMenuActivity.setActivityTitle("Open Source Licenses");
            startActivity(new Intent(this, OssLicensesMenuActivity.class));
        });

        btnBack.setOnClickListener(view -> {
            finish();
        });
    }
}

