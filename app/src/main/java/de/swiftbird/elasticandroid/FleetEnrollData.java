package de.swiftbird.elasticandroid;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * Represents the persistent data related to an agent's enrollment with a Fleet server.
 * This entity is stored in the local database and includes details necessary for managing
 * the agent's state and configuration as per the Fleet server's policy.
 */
@Entity
public class FleetEnrollData {
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

    @ColumnInfo(name = "verify_cert")
    public boolean verifyCert;

    @ColumnInfo(name = "fleet_certificate")
    public String fleetCertificate;

    @ColumnInfo(name = "action")
    public String action;

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
