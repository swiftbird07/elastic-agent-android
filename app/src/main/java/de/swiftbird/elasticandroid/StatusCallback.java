package de.swiftbird.elasticandroid;

/**
 * Callback interface for asynchronous operations that return a status.
 */
public interface StatusCallback {
    void onCallback(boolean success);
}
