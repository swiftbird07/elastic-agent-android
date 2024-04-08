package de.swiftbird.elasticandroid;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import org.json.JSONObject;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.MessageFormat;

import de.swiftbird.elasticandroid.R.id;

/**
 * HelpActivity provides a simple informational screen for the application.
 * It displays help for the end-user as well as administrators. It also displays the version of the application.
 * and its compatibility with the Elastic Agent.
 * Be aware that the static help text is written in the layout file.
 */
public class HelpActivity extends AppCompatActivity  {

    private TextView tvVersionInfo;
    private Button btnBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_help);
        btnBack = findViewById(id.btnBack);
        tvVersionInfo = findViewById(id.tvVersionInfo);

        tvVersionInfo.setText("App Version: " + BuildConfig.VERSION_NAME + "\nElastic Agent Compatibility: " + BuildConfig.AGENT_VERSION);

        btnBack.setOnClickListener(view -> {
            finish();
        });

    }

}