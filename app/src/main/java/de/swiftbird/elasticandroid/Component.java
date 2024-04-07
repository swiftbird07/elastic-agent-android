package de.swiftbird.elasticandroid;

import android.content.Context;
import java.util.List;

/**
 * Interface defining the common functionality of components within an Elastic Agent,
 * such as collecting, storing, and managing specific types of data (e.g., location, network).
 * Each component must be initialized with setup() before use, providing a consistent lifecycle across components.
 */
public interface Component {
    /**
     * Prepares the component for operation, initializing it with necessary data.
     *
     * @param context The application context.
     * @param enrollmentData Data regarding the agent's enrollment.
     * @param policyData Policy configurations affecting the component's behavior.
     * @param subComponent Optional identifier for sub-components.
     * @return True if setup was successful, false otherwise.
     */
    boolean setup(Context context, FleetEnrollData enrollmentData, PolicyData policyData, String subComponent);

    /**
     * Collects relevant events or data, as per the component's functionality.
     * This method will be called periodically by the agent's main loop (TODO: not yet implemented).
     * Notice: Most components will not need to implement this method as they will be event-driven.
     *
     * @param enrollmentData Data regarding the agent's enrollment.
     * @param policyData Policy configurations affecting data collection.
     */
    void collectEvents(FleetEnrollData enrollmentData, PolicyData policyData);

    /**
     * Adds a document to the component's internal buffer for later processing or transmission.
     *
     * @param document The document to add to the buffer.
     */
    void addDocumentToBuffer(ElasticDocument document);

    /**
     * Retrieves a list of documents from the buffer, up to a specified maximum number.
     *
     * @param maxDocuments The maximum number of documents to retrieve.
     * @return A list of documents from the buffer.
     */
    <T extends ElasticDocument> List<T> getDocumentsFromBuffer(int maxDocuments);

    /**
     * Gets the count of documents currently stored in the buffer.
     *
     * @return The number of documents in the buffer.
     */
    int getDocumentsInBufferCount();

    /**
     * Lists the permissions required by the component for its operation.
     *
     * @return A list of permissions required by the component.
     */
    List<String> getRequiredPermissions();

    /**
     * Returns the path name (without sub-component or parameters) associated with the policy configuration for the component.
     *
     * @return The component's path name.
     */
    String getPathName();

    /**
     * Disables the component, cleaning up resources and stopping any ongoing operations.
     *
     * @param context The application context.
     * @param enrollmentData Data regarding the agent's enrollment.
     * @param policyData Policy configurations affecting the component's behavior.
     */
    void disable(Context context, FleetEnrollData enrollmentData, PolicyData policyData);
}
