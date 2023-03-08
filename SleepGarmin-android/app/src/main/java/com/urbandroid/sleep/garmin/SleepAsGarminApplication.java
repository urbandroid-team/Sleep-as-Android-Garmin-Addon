package com.urbandroid.sleep.garmin;

import android.app.Application;

public class SleepAsGarminApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        GlobalInitializer.initializeIfRequired(this);
    }
}
