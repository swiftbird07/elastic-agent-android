package de.swiftbird.elasticandroid;

import androidx.room.ColumnInfo;
import androidx.room.TypeConverters;

import com.google.gson.annotations.SerializedName;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class ElasticDocument {

    @SerializedName("@timestamp")
    protected String timestamp;

    // Agent metadata
    @SerializedName("agent.ephemeral_id")
    protected String agentEphemeralId;

    @SerializedName("agent.id")
    protected String agentId;

    @SerializedName("agent.name")
    protected String agentName;

    @SerializedName("agent.type")
    protected String agentType;

    @SerializedName("agent.version")
    protected String agentVersion;



    // Host metadata
    @SerializedName("host.architecture")
    protected String hostArchitecture;

    @SerializedName("host.hostname")
    protected String hostHostname;

    @SerializedName("host.id")
    protected String hostId;

    @SerializedName("host.ip")
    @ColumnInfo(name = "hostIp")
    @TypeConverters(AppConverters.class)
    protected List<String>  hostIp;

    @SerializedName("source.ip")
    @ColumnInfo(name = "sourceIp")
    @TypeConverters(AppConverters.class)
    protected List<String>  sourceIp;

    @SerializedName("host.mac")
    protected String hostMac;

    @SerializedName("host.name")
    protected String hostName;

    @SerializedName("host.os.build")
    protected String hostOsBuild;

    @SerializedName("host.os.family")
    protected String hostOsFamily;

    @SerializedName("host.os.kernel")
    protected String hostOsKernel;

    @SerializedName("host.os.name")
    protected String hostOsName;

    @SerializedName("host.os.name.text")
    protected String hostOsNameText;

    @SerializedName("host.os.platform")
    protected String hostOsPlatform;

    @SerializedName("host.os.version")
    protected String hostOsVersion;

    @SerializedName("host.os.type")
    protected String hostOsType;


    // Component metadata
    @SerializedName("component.id")
    protected String componentId;

    @SerializedName("component.old_state")
    protected String componentOldState;

    @SerializedName("component.state")
    protected String componentState;

    // Data stream metadata

    @SerializedName("data_stream.dataset")
    protected String dataStreamDataset;

    @SerializedName("data_stream.namespace")
    protected String dataStreamNamespace;

    @SerializedName("data_stream.type")
    protected String dataStreamType;

    @SerializedName("ecs.version")
    protected String ecsVersion;

    @SerializedName("elastic_agent.id")
    protected String elasticAgentId;

    @SerializedName("elastic_agent.snapshot")
    protected boolean elasticAgentSnapshot;

    @SerializedName("elastic_agent.version")
    protected String elasticAgentVersion;

    @SerializedName("elastic_agent.id_status")
    protected String elasticAgentIdStatus;

    @SerializedName("event.dataset")
    protected String eventDataset;



    public ElasticDocument() {
        this.timestamp = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).format(new Date());
    }

    public ElasticDocument(FleetEnrollData enrollmentData, PolicyData policyData) {
        AgentMetadata metadata = AgentMetadata.getMetadataFromDeviceAndDB(enrollmentData.agentId, enrollmentData.hostname);

        // Parse agent metadata
        this.agentEphemeralId = "Unknown"; // TODO: Find out what this is
        this.agentId = enrollmentData.agentId;
        this.agentName = enrollmentData.hostname;
        this.agentType = "android";
        this.agentVersion = BuildConfig.AGENT_VERSION;

        // Parse host metadata
        this.hostArchitecture = metadata.getLocal().host.arch;
        this.hostHostname = metadata.getLocal().host.hostname;
        this.hostId = metadata.getLocal().host.id;

        this.hostIp = metadata.getLocal().host.ip;
        this.sourceIp = metadata.getLocal().host.ip;

        this.hostMac = metadata.getLocal().host.mac.get(0);
        this.hostName = metadata.getLocal().host.name;
        this.hostOsType = "android";
        this.hostOsBuild = metadata.getLocal().system.kernel;
        this.hostOsFamily = metadata.getLocal().system.family;
        this.hostOsKernel = metadata.getLocal().system.kernel;
        //this.hostOsName = metadata.getLocal().system.name; // DOES NOT WORK
        //this.hostOsNameText = metadata.getLocal().system.name; // DOES NOT WORK
        this.hostOsPlatform = metadata.getLocal().system.platform;
        this.hostOsVersion = metadata.getLocal().system.version;


        // Set the rest of the fields
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault()); // Locale.US to Locale.getDefault()
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC")); // This line ensures the time is in UTC
        this.timestamp = dateFormat.format(new Date());
        this.componentId = "default";
        this.componentOldState = "Healthy"; // TODO: Check if this is correct first
        this.componentState = "Healthy"; // True because otherwise the data would not be sent (at least in the current implementation)
        this.dataStreamDataset = policyData.dataStreamDataset;
        this.dataStreamNamespace = "default";
        this.dataStreamType = "logs";
        this.ecsVersion = "8.0.0"; // TODO: Make this a constant from build.gradle
        this.elasticAgentId = enrollmentData.agentId;
        this.elasticAgentSnapshot = false; // TODO: Maybe we can determine this from ES or Fleet
        this.elasticAgentVersion = BuildConfig.AGENT_VERSION;
        this.elasticAgentIdStatus = "verified"; // TODO: Check if we can determine this from ES or Fleet
        this.eventDataset = policyData.dataStreamDataset;
    }

}
