package de.swiftbird.elasticandroid;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;
import java.io.IOException;
import java.security.GeneralSecurityException;

/**
 * Provides secure storage for sensitive application preferences like the Fleet and Elastic API keys.
 * The preferences are stored in an encrypted form using the AndroidX Security library.
 */
public class AppSecurePreferences {
    private static final String FILE_NAME = "secure_prefs";
    private static final String ELASTIC_API_KEY_FIELD_NAME = "elastic_api_key";
    private static final String FLEET_API_KEY_FIELD_NAME = "fleet_api_key";
    private final SharedPreferences encryptedPreferences;
    private static AppSecurePreferences instance;

    /**
     * Gets the singleton instance of the AppSecurePreferences class.
     *
     * @param context The application context.
     * @return The singleton instance of the AppSecurePreferences class.
     */
    public static AppSecurePreferences getInstance(Context context) {
        if (instance == null) {
            try {
                instance = new AppSecurePreferences(context);
            } catch (GeneralSecurityException | IOException e) {
                AppLog.e("AppSecurePreferences", "Failed to create secure preferences", e);
                throw new RuntimeException("Failed to create secure preferences. Reason: " + e.getMessage() + " Cause: " + e.getCause());
            }
        }
        return instance;
    }

    /**
     * Initializes a new instance of the AppSecurePreferences class.
     *
     * @param context The application context.
     * @throws GeneralSecurityException If an error occurs during encryption.
     * @throws IOException              If an error occurs during I/O operations.
     */
    AppSecurePreferences(Context context) throws GeneralSecurityException, IOException {
        AppLog.i("AppSecurePreferences", "Creating secure preferences");
        MasterKey masterKey = new MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build();

        encryptedPreferences = EncryptedSharedPreferences.create(
                context,
                FILE_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM);
    }

    /**
     * Saves the Elastic API key to the secure preferences.
     *
     * @param apiKey The enrollment token to save.
     */
    public void saveElasticApiKey(String apiKey) {
        AppLog.i("AppSecurePreferences", "Saving Elastic API key to secure preferences");
        encryptedPreferences.edit().putString(ELASTIC_API_KEY_FIELD_NAME, apiKey).apply();
    }

    /**
     * Retrieves the Elastic API key from the secure preferences.
     *
     * @return The enrollment token if found; null otherwise.
     */
    public String getElasticApiKey() {
        AppLog.i("AppSecurePreferences", "Retrieving Elastic API key from secure preferences");
        return encryptedPreferences.getString(ELASTIC_API_KEY_FIELD_NAME, null);
    }

    /**
     * Saves the Fleet API key to the secure preferences.
     *
     * @param apiKey The API key to save.
     */
    public void saveFleetApiKey(String apiKey) {
        AppLog.i("AppSecurePreferences", "Saving Fleet API key to secure preferences");
        encryptedPreferences.edit().putString(FLEET_API_KEY_FIELD_NAME, apiKey).apply();
    }

    /**
     * Retrieves the Fleet API key from the secure preferences.
     *
     * @return The API key if found; null otherwise.
     */
    public String getFleetApiKey() {
        AppLog.i("AppSecurePreferences", "Retrieving Fleet API key from secure preferences");
        return encryptedPreferences.getString(FLEET_API_KEY_FIELD_NAME, null);
    }
}
