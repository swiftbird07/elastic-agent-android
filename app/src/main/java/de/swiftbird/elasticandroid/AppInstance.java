package de.swiftbird.elasticandroid;

import android.app.Application;
import android.content.Context;

/**
 * Extends the Application class to provide a singleton instance and application context accessible throughout the app.
 * This class ensures that a global application context is available, which can be useful for accessing resources,
 * starting activities, and more, from places not inherently tied to an activity's context.
 */
public class AppInstance extends Application {
    private static AppInstance instance;

    /**
     * Initializes the singleton application instance. This method is called when the application is starting,
     * before any other application objects have been created.
     */
    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
    }

    /**
     * Provides access to the singleton instance of the application.
     *
     * @return The singleton instance of AppInstance.
     */
    public static AppInstance getInstance() {
        return instance;
    }

    /**
     * Retrieves a context for the application.
     * This context is tied to the lifecycle of the application and can be used where an activity context is not available.
     *
     * @return The application context for global use.
     */
    public static Context getAppContext() {
        return instance.getApplicationContext();
    }
}