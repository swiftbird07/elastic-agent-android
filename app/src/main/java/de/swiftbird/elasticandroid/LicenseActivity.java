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

    private Button btnBack;
    private Button btnOSSLicenses;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_licenses);
        btnBack = findViewById(R.id.btnBack);
        btnOSSLicenses = findViewById(R.id.btnOSSLicenses);

        TextView tvLicenseText = findViewById(R.id.tvLicenseText);
        String mitLicenseText = "MIT License\n\n" +
                "Copyright (c) 2024 Martin Offermann (@swiftbird07) \n\n" +
                "Permission is hereby granted, free of charge, to any person obtaining a copy\n" +
                "of this software and associated documentation files (the \"Software\"), to deal\n" +
                "in the Software without restriction, including without limitation the rights\n" +
                "to use, copy, modify, merge, publish, distribute, sublicense, and/or sell\n" +
                "copies of the Software, and to permit persons to whom the Software is\n" +
                "furnished to do so, subject to the following conditions:\n\n" +
                "The above copyright notice and this permission notice shall be included in all\n" +
                "copies or substantial portions of the Software.\n\n" +
                "THE SOFTWARE IS PROVIDED \"AS IS\", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR\n" +
                "IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,\n" +
                "FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE\n" +
                "AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER\n" +
                "LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,\n" +
                "OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE\n" +
                "SOFTWARE.";

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

