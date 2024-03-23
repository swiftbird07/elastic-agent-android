package de.swiftbird.elasticandroid;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity
public class EnrollmentData {
    @PrimaryKey
    @ColumnInfo(name = "id")
    public int id;

    @ColumnInfo(name = "agent_id")
    public String agentId;

    @ColumnInfo(name = "is_enrolled")
    public boolean isEnrolled;

    @ColumnInfo(name = "hostname")
    public String hostname;

    @ColumnInfo(name = "fleet_url")
    public String fleetUrl;

    @ColumnInfo(name = "veryify_cert")
    public boolean verifyCert;

    @ColumnInfo(name = "action")
    public String action;

    @ColumnInfo(name = "access_api_key")
    public String accessApiKey;

    @ColumnInfo(name = "access_api_key_id")
    public String accessApiKeyId;

    @ColumnInfo(name = "active")
    public boolean active;

    @ColumnInfo(name = "enrolled_at")
    public String enrolledAt;

    @ColumnInfo(name = "policy_id")
    public String policyId;

    @ColumnInfo(name = "status")
    public String status;

    @ColumnInfo(name = "type")
    public String type;

    @ColumnInfo(name = "last_checkin")
    public String lastCheckin;

    @ColumnInfo(name = "last_policy_update")
    public String lastPolicyUpdate;

    @ColumnInfo(name = "policy")
    public String policy;
}
