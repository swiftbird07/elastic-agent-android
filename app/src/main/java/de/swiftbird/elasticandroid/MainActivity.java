package de.swiftbird.elasticandroid;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;

import com.google.android.material.snackbar.Snackbar;

import androidx.appcompat.app.AppCompatActivity;

import android.view.View;

import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import de.swiftbird.elasticandroid.databinding.ActivityMainBinding;

import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {

    private TextView tvEnrollmentInfo;
    private Button btnEnrollUnenroll;
    private TextView tvLastDataSent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvEnrollmentInfo = findViewById(R.id.tvEnrollmentInfo);
        btnEnrollUnenroll = findViewById(R.id.btnEnrollUnenroll);
        tvLastDataSent = findViewById(R.id.tvLastDataSent);

        updateUIBasedOnEnrollment();

        btnEnrollUnenroll.setOnClickListener(view -> {
            if (isEnrolled()) {
                showUnenrollmentDialog();
            } else {
                startActivity(new Intent(MainActivity.this, EnrollmentActivity.class));
            }
        });
    }

    private boolean isEnrolled() {
        // Check enrollment status (for now, assume false)
        return false;
    }

    private void updateUIBasedOnEnrollment() {
        if (isEnrolled()) {
            tvEnrollmentInfo.setText("Enrolled");
            btnEnrollUnenroll.setText("Unenroll Agent");
        } else {
            tvEnrollmentInfo.setText("Not Enrolled");
            btnEnrollUnenroll.setText("Enroll Agent");
        }
    }

    private void showUnenrollmentDialog() {
        new AlertDialog.Builder(this)
                .setMessage("Are you sure you want to unenroll?")
                .setPositiveButton("Yes", (dialog, which) -> unenrollAgent())
                .setNegativeButton("No", null)
                .show();
    }

    private void unenrollAgent() {
        // Reset enrollment status and update UI
        updateUIBasedOnEnrollment();
    }
}
