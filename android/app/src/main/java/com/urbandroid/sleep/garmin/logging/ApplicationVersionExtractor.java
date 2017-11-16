package com.urbandroid.sleep.garmin.logging;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

import com.urbandroid.sleep.garmin.logging.ApplicationVersionInfo;

/**
 * Created by artaud on 16.11.17.
 */

public class ApplicationVersionExtractor {
    public ApplicationVersionInfo getCurrentVersion(Context context) {
        return getCurrentVersion(context, context.getPackageName());
    }

    public ApplicationVersionInfo getCurrentVersion(Context context, String packageName) {
        PackageManager pm = context.getPackageManager();
        PackageInfo pi;
        try {
            pi = pm.getPackageInfo(packageName, 0);
        } catch (PackageManager.NameNotFoundException eNnf) {
            //doubt this will ever run since we want info about our own package
            pi = new PackageInfo();
            pi.versionName = "Unknown";
            pi.versionCode = 0;
        }

        return new ApplicationVersionInfo(pi.versionCode, pi.versionName);
    }
}
