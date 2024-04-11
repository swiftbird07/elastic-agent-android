package de.swiftbird.elasticandroid;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

/**
 * LegalActivity displays the legal disclaimer for the application.
 * It provides information about the software's warranty, liability, and prohibited uses.
 */
public class LegalActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_legal);
        Button btnBack = findViewById(R.id.btnBack);
        String legalDisclaimerText = getString(R.string.legal_disclaimer_text);

        TextView tvLegalText = findViewById(R.id.tvLegalText);
        tvLegalText.setText(getString(R.string.legal_disclaimer_text));
        tvLegalText.setText(legalDisclaimerText);

        btnBack.setOnClickListener(view -> {
            finish();
        });
    }
}

