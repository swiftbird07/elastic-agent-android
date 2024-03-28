package de.swiftbird.elasticandroid;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class LegalActivity extends AppCompatActivity {

    private Button btnBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_legal);
        btnBack = findViewById(R.id.btnBack);

        TextView tvLegalText = findViewById(R.id.tvLegalText);
        String legalDisclaimerText =
                "This software is provided 'as is', without warranty of any kind, express or " +
                        "implied, including but not limited to the warranties of merchantability, " +
                        "fitness for a particular purpose and noninfringement. In no event shall the " +
                        "authors or copyright holders be liable for any claim, damages or other " +
                        "liability, whether in an action of contract, tort or otherwise, arising from, " +
                        "out of or in connection with the software or the use or other dealings in the " +
                        "software.\n\n" +
                        "It is strictly prohibited to use this software for illegal activities, including " +
                        "but not limited to spying on individuals without their explicit consent. All users " +
                        "and individuals who have the software installed on their device must be duly informed " +
                        "about its functionalities and purposes.\n\n" +
                        "The developer of this software disclaims all liability for misuse or illegal use " +
                        "of the software. Users are responsible for ensuring their use of the software complies " +
                        "with all applicable laws and regulations.";

        tvLegalText.setText(legalDisclaimerText);

        btnBack.setOnClickListener(view -> {
            finish();
        });
    }
}

