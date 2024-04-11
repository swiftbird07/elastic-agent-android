package de.swiftbird.elasticandroid;

import com.google.gson.annotations.SerializedName;
import java.util.List;
import java.util.Map;

/**
 * Models the response received from a Fleet server upon an agent's check-in.
 * This response includes any actions the server requires the agent to take,
 * such as applying new policy configurations, performing diagnostics, or other
 * operational commands.
 */
public class FleetCheckinResponse {
    @SerializedName("ack_token")
    protected String ackToken;

    @SerializedName("action")
    private String action;

    @SerializedName("actions")
    private List<Action> actions;

    protected  List<Action> getActions() {
        return actions;
    }

    /**
     * Represents an individual action item within the check-in response.
     * Actions can include policy updates, diagnostic requests, or other commands.
     */
    public static class Action {
        @SerializedName("agent_id")
        private String agentId;

        @SerializedName("created_at")
        private String createdAt;

        @SerializedName("data")
        private PolicyData data;

        @SerializedName("id")
        private String id;

        @SerializedName("type")
        private String type;

        protected String getCreatedAt() {
            return createdAt;
        }

        protected PolicyData getData() {
            return data;
        }

        protected String getId() {return id; }

        public String getType() {
            return type;
        }

        /**
         * Contains the detailed policy data associated with a POLICY_CHANGE action.
         * This includes the agent configuration, input streams, and output settings.
         */
        public static class PolicyData {
            @SerializedName("policy")
            private Policy policy;

            protected Policy getPolicy() {
                return policy;
            }

            public static class Policy {
                @SerializedName("agent")
                private Agent agent;

                @SerializedName("id")
                private String id;

                @SerializedName("inputs")
                private List<Input> inputs;

                @SerializedName("output_permissions")
                private Map<String, OutputPermissions> outputPermissions;

                @SerializedName("outputs")
                private Map<String, Output> outputs;

                @SerializedName("revision")
                private int revision;

                @SerializedName("signed")
                private Signed signed;

                protected Agent getAgent() {
                    return agent;
                }

                protected List<Input> getInputs() {
                    return inputs;
                }

                protected Map<String, Output> getOutputs() {
                    return outputs;
                }

                protected int getRevision() {
                    return revision;
                }

                public static class OutputPermissions {
                    @SerializedName("default")
                    private Permissions defaultPermissions;

                    public static class Permissions {
                        @SerializedName("_elastic_agent_monitoring")
                        private ElasticAgentMonitoring elasticAgentMonitoring;

                        @SerializedName("_elastic_agent_checks")
                        private ElasticAgentChecks elasticAgentChecks;
                    }

                    public static class ElasticAgentMonitoring {
                        @SerializedName("indices")
                        private List<String> indices;
                    }

                    public static class ElasticAgentChecks {
                        @SerializedName("cluster")
                        private List<String> cluster;
                    }
                }

                /**
                 * Defines an output target for the agent, typically an Elasticsearch
                 * cluster, including connection and authentication details.
                 */
                public static class Output {
                    @SerializedName("api_key")
                    private String apiKey;

                    @SerializedName("hosts")
                    private List<String> hosts;

                    @SerializedName("ssl.ca_trusted_fingerprint")
                    private String sslCaTrustedFingerprint;

                    @SerializedName("ssl.certificate_authorities")
                    private List<String> sslCertificateAuthorities;

                    @SerializedName("type")
                    private String type;

                    protected String getApiKey() {
                        return apiKey;
                    }

                    protected List<String> getHosts() {
                        return hosts;
                    }

                    protected String getSslCaTrustedFingerprint() {
                        return sslCaTrustedFingerprint;
                    }

                    protected List<String> getSslCertificateAuthorities() {
                        return sslCertificateAuthorities;
                    }
                }

                /**
                 * Contains the signed policy data, including the data and signature.
                 */
                public static class Signed {
                    @SerializedName("data")
                    private String data;

                    @SerializedName("signature")
                    private String signature;

                }
            }
        }

        /**
         * Contains the detailed agent configuration, including protection settings
         * and other agent-specific details.
         */
        public static class Agent {
            @SerializedName("protection")
            private Protection protection;

            protected Protection getProtection() {
                return protection;
            }
        }


        /**
         * Contains the protection settings for the agent, including the uninstall
         * token hash and whether protection is enabled.
         */
        public static class Protection {
            @SerializedName("enabled")
            private boolean enabled;

            @SerializedName("uninstall_token_hash")
            private String uninstallTokenHash;

            protected boolean getEnabled() {
                return enabled;
            }

            protected String getUninstallTokenHash() {
                return uninstallTokenHash;
            }
        }


        /**
         * Defines an input within the policy, potentially including multiple
         * data streams for the agent to monitor and report.
         */
        public static class Input {
            @SerializedName("type")
            private String type;

            @SerializedName("streams")
            private List<Stream> streams;

            @SerializedName("data_stream")
            private DataStream dataStream;

            @SerializedName("id")
            private String id;

            @SerializedName("meta")
            private Meta meta;

            @SerializedName("name")
            private String name;

            @SerializedName("package_policy_id")
            private String packagePolicyId;

            @SerializedName("revision")
            private int revision;

            @SerializedName("use_output")
            private String useOutput;

            protected List<Stream> getStreams() {
                return streams;
            }

            protected Meta getMeta() {
                return meta;
            }

            protected String getName() {
                return name;
            }

            public static class Meta {
                @SerializedName("package")
                private Package packageInfo;

                public static class Package {
                    @SerializedName("name")
                    private String name;

                    @SerializedName("version")
                    private String version;

                    protected String getName() {
                        return name;
                    }

                    protected String getVersion() {
                        return version;
                    }
                }

                protected Package getPackageInfo() {
                    return packageInfo;
                }
            }

            /**
             * Represents a single stream within an input, detailing the specific
             * data collection or reporting behavior expected of the agent.
             */
            public static class Stream {
                @SerializedName("data_stream")
                private DataStream dataStream;

                @SerializedName("id")
                private String id;

                @SerializedName("ignore_older")
                private String ignoreOlder;

                @SerializedName("interval")
                private String interval;

                // Custom
                @SerializedName("es_request_interval")
                private String put_interval;

                @SerializedName("max_documents_per_request")
                private int maxDocumentsPerRequest;


                @SerializedName("checkin_interval")
                private String checkin_interval;

                @SerializedName("paths")
                private List<String> paths;

                @SerializedName("allow_user_unenroll")
                private boolean allowUserUnenroll;

                @SerializedName("use_backoff")
                private boolean useBackoff;

               @SerializedName("max_backoff_interval")
                private String maxBackoffInterval;

               @SerializedName("backoff_on_empty_buffer")
                private boolean backoffOnEmptyBuffer;

               @SerializedName("disable_on_low_battery")
                private boolean disableIfBatteryLow;

                protected DataStream getDataStream() {
                    return dataStream;
                }

                protected String getIgnoreOlder() {
                    return ignoreOlder;
                }

                protected List<String> getPaths() {
                    return paths;
                }

                protected boolean getAllowUserUnenroll() {
                    return allowUserUnenroll;
                }

                protected String getPutInterval() {
                    return put_interval;
                }

                protected String getCheckinInterval() {
                    return checkin_interval;
                }

                protected int getMaxDocumentsPerRequest() {
                    return maxDocumentsPerRequest;
                }

                public boolean getUseBackoff() { return useBackoff; }

                public String getMaxBackoffInterval() { return maxBackoffInterval; }

                public boolean getBackoffOnEmptyBuffer() { return backoffOnEmptyBuffer; }

                public boolean getDisableIfBatteryLow() { return disableIfBatteryLow; }
            }


            /**
             * Details the Elasticsearch data stream to which collected data
             * should be reported.
             */
            public static class DataStream {
                @SerializedName("dataset")
                private String dataset;

                @SerializedName("namespace")
                private String namespace;

                protected String getDataset() {
                    return dataset;
                }
            }
        }
    }
}
