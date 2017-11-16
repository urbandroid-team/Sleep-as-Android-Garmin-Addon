package com.urbandroid.sleep.garmin.logging;

/**
 * Created by artaud on 16.11.17.
 */

public class ApplicationVersionInfo {
    private final int versionCode;
    private final String versionName;

    public ApplicationVersionInfo(int versionCode, String versionName) {
        this.versionCode = versionCode;
        this.versionName = versionName;
    }

    public int getVersionCode() {
        return versionCode;
    }

    public String getVersionName() {
        return versionName;
    }

    @Override
    public String toString() {
        return "Version code: " + versionCode + " Name: " + versionName;
    }
}


