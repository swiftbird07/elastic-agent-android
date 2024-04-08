package de.swiftbird.elasticandroid;

/**
 * Represents the response received when querying the status of the Fleet server.
 * It contains the health and availability of the Fleet server.
 */
public class FleetStatusResponse {
    private String name;
    private String status;

    public FleetStatusResponse(String healthy) {
        this.status = healthy;
    }

    // Getters and setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
