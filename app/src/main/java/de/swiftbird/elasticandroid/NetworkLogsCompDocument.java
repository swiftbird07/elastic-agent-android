package de.swiftbird.elasticandroid;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters;

import com.google.gson.annotations.SerializedName;

import java.net.InetAddress;
import java.util.List;
import java.util.Objects;

/**
 * Represents network log events captured by the application, including DNS queries and TCP connections.
 * This entity is stored in the application's Room database and is used to aggregate network events
 * before processing or transmission to a centralized logging system.
 *
 * <p>The class extends {@link ElasticDocument} to include common Elastic Stack fields and additional
 * network-specific information such as DNS query names, resolved IP addresses, and TCP connection details.</p>
 *
 * <p>Fields are annotated for serialization into JSON using {@link com.google.gson.annotations.SerializedName}
 * and for storage in the SQLite database using Room annotations.</p>
 */
@Entity
public class NetworkLogsCompDocument extends ElasticDocument {
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    public int id;

    @SerializedName("event.action")
    @ColumnInfo(name = "eventAction")
    public String eventAction;

    @SerializedName("event.category")
    @ColumnInfo(name = "eventCategory")
    public String eventCategory;

    @SerializedName("network.protocol")
    @ColumnInfo(name = "networkProtocol")
    public String networkProtocol;

    @SerializedName("network.transport")
    @ColumnInfo(name = "networkTransport")
    public String networkTransport;


    @SerializedName("app.package.name")
    @ColumnInfo(name = "appPackageName")
    public String appPackageName;

    @SerializedName("process.name") // This is not really the process name, but it is similar in this context (and we can't get any real process name without root anyway)
    @ColumnInfo(name = "processName")
    public String processName;

    @SerializedName("dns.question.name")
    @ColumnInfo(name = "dnsQuestionName")
    public String dnsQuestionName;

    @SerializedName("dns.question.type")
    @ColumnInfo(name = "dnsQuestionType")
    public String dnsQuestionType;

    @SerializedName("suricata.eve.dns.rrname") // This is not ECS, but helpful to aggregate with Suricata logs, if available
    @ColumnInfo(name = "suricataEveDNSRrname")
    public String suricataEveDNSRrname;

    @SerializedName("dns.resolved_ip") // This is a confusing ECS field name, as it can contain multiple IPs
    @ColumnInfo(name = "dnsResolvedIP")
    @TypeConverters(AppConverters.class)
    public List<InetAddress> resolvedIPs;

    @SerializedName("suricata.eve.dns.answers.rdata") // This is not ECS, but helpful to aggregate with Suricata logs, if available
    @ColumnInfo(name = "suricataEveDNSAnswersRdata")
    @TypeConverters(AppConverters.class)
    public List<InetAddress> suricataEveDNSAnswersRdata;

    @SerializedName("destination.ip")
    @ColumnInfo(name = "destinationIP")
    public String destinationIP;

    @SerializedName("destination.port")
    @ColumnInfo(name = "destinationPort")
    public int destinationPort;


    @SerializedName("message")
    @ColumnInfo(name = "message")
    public String message;

    // We use a few placeholder values here to better display DNS events in the Kibana timeline
    @SerializedName("user.name")
    @ColumnInfo(name = "userName", defaultValue = "user")
    public final static String userName = "user"; // this is actually quite accurate as android apps run as "user" mostly
    @SerializedName("user.domain")
    @ColumnInfo(name = "userDomain", defaultValue = "unknown")
    public final static String userDomain = "unknown";
    @SerializedName("process.pid")
    @ColumnInfo(name = "processPid", defaultValue = "1")
    public final static int processPid = 1;
    @SerializedName("winlog.event_id")
    @ColumnInfo(name = "winlogEventId", defaultValue = "1")
    public final static int winlogEventId = 1;


    public NetworkLogsCompDocument() {}


    /**
     * Constructs a new {@code NetworkLogsCompDocument} for a DNS query event.
     *
     * @param enrollmentData The enrollment data of the agent.
     * @param policyData The policy data associated with the agent.
     * @param type The type of the network event, expected to be "DNS" for this constructor.
     * @param packageName The package name of the app initiating the DNS query.
     * @param hostname The hostname being queried.
     * @param inetAddresses The list of {@link InetAddress} objects representing resolved IP addresses for the hostname.
     * @param message Additional message or context about the DNS event.
     * @throws IllegalArgumentException if the event type is not "DNS".
     */
    public NetworkLogsCompDocument(FleetEnrollData enrollmentData, PolicyData policyData, String type, String packageName, String hostname, List<InetAddress> inetAddresses, String message){
        super(enrollmentData, policyData);

        // Type: QUERY
        if (type == null || !type.equals("DNS")) {
            throw new IllegalArgumentException("Invalid type for NetworkLogsCompDocument: " + type);
        }

        this.networkProtocol = "dns";
        this.networkTransport = "udp";

        this.appPackageName = packageName;
        // Use the last part of package name as process name and add ".apk" to indicate it is an app
        this.processName = packageName.substring(packageName.lastIndexOf('.') + 1) + ".apk";

        this.dnsQuestionName = hostname;
        this.suricataEveDNSRrname = hostname;
        this.resolvedIPs = inetAddresses;

        // Set dns question type to "A" if we have resolved IPs, otherwise to "PTR". Set it to "AAAA" if we have IPv6 addresses.
        if (!inetAddresses.isEmpty()) {
            this.dnsQuestionType = Objects.requireNonNull(inetAddresses.get(0).getHostAddress()).contains(":") ? "AAAA" : "A";
        } else {
            this.dnsQuestionType = "PTR";
        }

        this.suricataEveDNSAnswersRdata = inetAddresses;
        this.message = message;
        this.eventAction = "query";
        this.eventCategory = "network";
    }

    /**
     * Constructs a new {@code NetworkLogsCompDocument} for a TCP connection event.
     *
     * @param enrollmentData The enrollment data of the agent.
     * @param policyData The policy data associated with the agent.
     * @param type The type of the network event, expected to be "CONNECT" for this constructor.
     * @param packageName The package name of the app initiating the connection.
     * @param inetAddress The {@link InetAddress} of the destination IP address.
     * @param port The destination port of the connection.
     * @param message Additional message or context about the TCP connection event.
     * @throws IllegalArgumentException if the event type is not "CONNECT".
     */
    public NetworkLogsCompDocument(FleetEnrollData enrollmentData, PolicyData policyData, String type, String packageName, InetAddress inetAddress, int port, String message){
        super(enrollmentData, policyData);

        // Type: CONNECT
        if (type == null || !type.equals("CONNECT")) {
            throw new IllegalArgumentException("Invalid type for NetworkLogsCompDocument: " + type);
        }

        this.networkProtocol = "tcp";
        this.networkTransport = "tcp";

        this.appPackageName = packageName;
        // Use the last part of package name as process name and add ".apk" to indicate it is an app
        this.processName = packageName.substring(packageName.lastIndexOf('.') + 1) + ".apk";

        // Parse destination IP as string but remove "/" at the beginning if present
        this.destinationIP = inetAddress.toString().replace("/", "");

        this.destinationPort = port;
        this.message = message;
        this.eventAction = "network_flow";
        this.eventCategory = "network";
    }
}

