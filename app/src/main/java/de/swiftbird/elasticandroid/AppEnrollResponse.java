package de.swiftbird.elasticandroid;

/**
 * Represents the response received from the server upon the enrollment request of the application.
 * This class encapsulates the response data, including details such as agent ID, agent name, a message indicating
 * the outcome of the enrollment process, and the status of the enrollment.
 */
public class AppEnrollResponse {
    private Data data;

    // Getter and setter
    public Data getData() {
        return data;
    }

    public void setData(Data data) {
        this.data = data;
    }

    /**
     * Inner class representing the data structure of the enrollment response.
     * Contains the agent's ID, name, a message regarding the enrollment process, and its status.
     */
    public static class Data {
        private String agent_id;
        private String agent_name;
        private String message;
        private String status;

        // Getters and Setters
        public String getAgentId() {
            return agent_id;
        }

        public void setAgentId(String agent_id) {
            this.agent_id = agent_id;
        }

        public String getAgentName() {
            return agent_name;
        }

        public void setAgentName(String agent_name) {
            this.agent_name = agent_name;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }
    }
}