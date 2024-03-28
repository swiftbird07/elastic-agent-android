package de.swiftbird.elasticandroid;

import android.os.Build;
import android.util.Log;

import androidx.annotation.Nullable;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

public class AgentMetadata {

        private String user_provided;
        private Local local;
        private List<String> tags;

        public String getUser_provided() {
            return user_provided;
        }

        public void setUser_provided(String user_provided) {
            this.user_provided = user_provided;
        }

        public Local getLocal() {
            return local;
        }

        public void setLocal(Local local) {
            this.local = local;
        }

        public List<String> getTags() {
            return tags;
        }

        public void setTags(List<String> tags) {
            this.tags = tags;
        }

    public AgentMetadata(Local local) {
        this.local = local;
    }

    public static class Local {
        @SerializedName("elastic")
        public ElasticECSMeta elastic;

        @SerializedName("host")
        public HostECSMeta host;

        @SerializedName("system")
        public SystemECSMeta system;

        public Local(ElasticECSMeta elastic, HostECSMeta host, SystemECSMeta system) {
            this.elastic = elastic;
            this.host = host;
            this.system = system;
        }
    }

    public static class ElasticECSMeta {
        @SerializedName("agent")
        public AgentECSMeta agent;

        public ElasticECSMeta(AgentECSMeta agent) {
            this.agent = agent;
        }
    }

    public static class AgentECSMeta {
        @SerializedName("id")
        public String id;

        @SerializedName("version")
        public String version;

        @SerializedName("snapshot")
        public boolean snapshot;

        @SerializedName("build.original")
        public String buildOriginal;

        @SerializedName("upgradeable")
        public boolean upgradeable;

        @SerializedName("log_level")
        public String logLevel;

        @SerializedName("complete")
        public boolean complete;
    }

    public static class HostECSMeta {
        @SerializedName("architecture")
        public String arch;

        @SerializedName("hostname")
        public String hostname;

        @SerializedName("name")
        public String name;

        @SerializedName("id")
        public String id;

        @SerializedName("ip")
        public List<String> ip;

        @SerializedName("mac")
        public List<String> mac;
    }

    public static class SystemECSMeta {
        @SerializedName("family")
        public String family;

        @SerializedName("kernel")
        public String kernel;

        @SerializedName("platform")
        public String platform;

        @SerializedName("version")
        public String version;

        @SerializedName("name")
        public String name;

        @SerializedName("full")
        public String fullName;
    }

    public static AgentMetadata getMetadataFromDeviceAndDB(@Nullable String agentId, String hostname) {
        // Agent metadata from provided data
        AgentECSMeta agentECSMeta = new AgentECSMeta();
        if (agentId != null) {
            agentECSMeta.id = agentId;
        }
        agentECSMeta.version = BuildConfig.AGENT_VERSION;
        agentECSMeta.snapshot = false;
        agentECSMeta.buildOriginal = BuildConfig.AGENT_VERSION; // TODO: Replace with actual build version
        agentECSMeta.upgradeable = false;
        agentECSMeta.logLevel = "info";
        agentECSMeta.complete = false;

        ElasticECSMeta elasticECSMeta = new ElasticECSMeta(agentECSMeta);

        // Host metadata using Android's Build class
        HostECSMeta hostECSMeta = new HostECSMeta();
        hostECSMeta.arch = Build.SUPPORTED_ABIS[0]; // Primary ABI
        hostECSMeta.hostname = hostname;
        hostECSMeta.name = hostname;
        hostECSMeta.id = Build.ID; // A build ID, not necessarily unique per device

        hostECSMeta.ip = getDeviceIPs(); // Get all non-loopback IP addresses
        hostECSMeta.mac = Collections.singletonList("02:00:00:00:00:00"); // Android doesn't provide any other MAC address anyway

        // System metadata with static/hardcoded values
        SystemECSMeta systemECSMeta = new SystemECSMeta();
        systemECSMeta.family = "Android";
        systemECSMeta.kernel = System.getProperty("os.version", "N/A"); // Kernel version, if accessible
        systemECSMeta.platform = "Android";
        systemECSMeta.version = Build.VERSION.RELEASE; // OS version
        systemECSMeta.name = "Android"; // System name
        systemECSMeta.fullName = Build.MODEL + " " + Build.VERSION.RELEASE; // Concatenated model and version

        // Construct and return the LocalMeta object with filled-in metadata
        AgentMetadata metadata = new AgentMetadata(new Local(elasticECSMeta, hostECSMeta, systemECSMeta));
        Log.d("AgentMetadata", "Gathered Agent metadata: " + new Gson().toJson(metadata)); // Log the metadata object

        return metadata;
    }

    public static List<String> getDeviceIPs() {
        List<String> ipAddresses = new ArrayList<>();
        try {
            Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
            while (networkInterfaces.hasMoreElements()) {
                NetworkInterface networkInterface = networkInterfaces.nextElement();
                Enumeration<InetAddress> inetAddresses = networkInterface.getInetAddresses();
                while (inetAddresses.hasMoreElements()) {
                    InetAddress inetAddress = inetAddresses.nextElement();
                    if (!inetAddress.isLoopbackAddress()) {
                        ipAddresses.add(inetAddress.getHostAddress());
                    }
                }
            }
        } catch (Exception e) {
            Log.e("AgentMetadata", "Error fetching IP addresses", e);
        }
        return ipAddresses;
    }


}
