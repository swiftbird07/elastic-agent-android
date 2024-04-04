package de.swiftbird.elasticandroid;

import android.app.Application;
import android.content.Context;

public class AppInstance extends Application {
    private static AppInstance instance;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
    }

    public static AppInstance getInstance() {
        return instance;
    }

    public static Context getAppContext() {
        return instance.getApplicationContext();
    }
}