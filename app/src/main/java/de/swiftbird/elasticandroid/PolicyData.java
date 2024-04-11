package de.swiftbird.elasticandroid;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * Represents the policy data entity stored in the local database. This entity encapsulates
 * all policy-related configurations received from the fleet server, including intervals
 * for check-ins and data submission, protection settings, and data stream configurations.
 *
 * <p>This entity is crucial for ensuring the app operates in alignment with the current
 * policy set by the fleet server, allowing for dynamic adjustment of behavior
 * based on received policies.</p>
 *
 */
@Entity
public class PolicyData {
    @PrimaryKey(autoGenerate = true)
    public int id;

    // Time when the policy was created on the fleet server.
    @ColumnInfo(name = "created_at")
    public String createdAt;

    // Revision number to track policy updates.
    @ColumnInfo(name = "revision")
    public int revision;

    // Flag indicating if device protection is enabled.
    @ColumnInfo(name = "protection_enabled")
    public boolean protectionEnabled;

    // Hash of the uninstallation token, used for secure removal.
    @ColumnInfo(name = "uninstall_token_hash")
    public String uninstallTokenHash;

    // Name of the input configured in the policy.
    @ColumnInfo(name = "input_name")
    public String inputName;

    // Determines whether users can unenroll the device.
    @ColumnInfo(name = "allow_user_unenroll")
    public boolean allowUserUnenroll;

    // Identifies the log package name- and version for data submission.
    @ColumnInfo(name = "log_package_name")
    public String logPackageName;

    @ColumnInfo(name = "log_package_version")
    public String logPackageVersion;

    // Identifies the data stream dataset for document submission.
    @ColumnInfo(name = "data_stream_dataset")
    public String dataStreamDataset;

    // Configures how old data can be before being ignored (not used).
    @ColumnInfo(name = "ignore_older")
    public String ignoreOlder;

    // Generic interval setting, context-specific use (not used).
    @ColumnInfo(name = "interval")
    public String interval;

    // Regular interval for check-in with the fleet server.
    @ColumnInfo(name = "checkin_interval")
    public int checkinInterval;

    // Backoff interval for check-in attempts after failures.
    @ColumnInfo(name = "backoff_checkin_interval")
    public int backoffCheckinInterval;

    // Interval for submitting data to the Elasticsearch data stream index.
    @ColumnInfo(name = "put_interval")
    public int putInterval;

    // Backoff interval for data submission after failures.
    @ColumnInfo(name = "backoff_put_interval")
    public int backoffPutInterval;

    // Maximum number of documents to submit per request.
    @ColumnInfo(name = "max_documents_per_request")
    public int maxDocumentsPerRequest;

    // Disable data submission when the device has low battery.
    @ColumnInfo(name = "disable_if_battery_low")
    public boolean disableIfBatteryLow;

    // Concatenated list of component paths included in the policy.
    @ColumnInfo(name = "paths")
    public String paths;

    // Elasticsearch host for data submission.
    @ColumnInfo(name = "hosts")
    public String hosts;

    // Fingerprint for SSL certificate verification.
    @ColumnInfo(name = "ssl_ca_trusted_fingerprint")
    public String sslCaTrustedFingerprint;

    // Full SSL certificate for establishing trust.
    @ColumnInfo(name = "ssl_ca_trusted_full")
    public String sslCaTrustedFull;

    // Flag to enable backoff strategy.
    @ColumnInfo(name = "use_backoff")
    public boolean useBackoff;

    // Maximum interval for backoff strategy.
    @ColumnInfo(name = "max_backoff_interval")
    public int maxBackoffInterval;

    // Enables backoff when the buffer is empty.
    @ColumnInfo(name = "backoff_on_empty_buffer")
    public boolean backoffOnEmptyBuffer;

    // Identifier for the output policy configuration.
    @ColumnInfo(name = "output_policy_id")
    public String outputPolicyId;

    // Last time the policy was updated.
    @ColumnInfo(name = "last_updated")
    public String lastUpdated;

    // Identifier for the check-in action, used for acknowledgments.
    @ColumnInfo(name = "checkin_action_id")
    public String actionId;
}