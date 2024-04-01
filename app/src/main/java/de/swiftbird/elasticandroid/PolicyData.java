package de.swiftbird.elasticandroid;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity
public class PolicyData {
    @PrimaryKey(autoGenerate = true)
    public int id;

    @ColumnInfo(name = "created_at")
    public String createdAt;

    @ColumnInfo(name = "revision")
    public int revision;

    @ColumnInfo(name = "protection_enabled")
    public boolean protectionEnabled;

    @ColumnInfo(name = "uninstall_token_hash")
    public String uninstallTokenHash;

    @ColumnInfo(name = "input_name")
    public String inputName;

    @ColumnInfo(name = "allow_user_unenroll")
    public boolean allowUserUnenroll;

    @ColumnInfo(name = "data_stream_dataset")
    public String dataStreamDataset;

    @ColumnInfo(name = "ignore_older")
    public String ignoreOlder;

    @ColumnInfo(name = "interval")
    public String interval;

    @ColumnInfo(name = "checkin_interval")
    public int checkinInterval;

    @ColumnInfo(name = "backoff_checkin_interval")
    public int backoffCheckinInterval;

    @ColumnInfo(name = "put_interval")
    public int putInterval;

    @ColumnInfo(name = "backoff_put_interval")
    public int backoffPutInterval;

    @ColumnInfo(name = "max_documents_per_request")
    public int maxDocumentsPerRequest;

    // Paths will be stored as a single String, concatenated with a "," delimiter
    @ColumnInfo(name = "paths")
    public String paths;

    @ColumnInfo(name = "api_key")
    public String apiKey;

    @ColumnInfo(name = "hosts")
    public String hosts;

    @ColumnInfo(name = "ssl_ca_trusted_fingerprint")
    public String sslCaTrustedFingerprint;

    @ColumnInfo(name = "output_policy_id")
    public String outputPolicyId;

    @ColumnInfo(name = "last_updated")
    public String lastUpdated;

    @ColumnInfo(name = "checkin_action_id")
    public String actionId;
}