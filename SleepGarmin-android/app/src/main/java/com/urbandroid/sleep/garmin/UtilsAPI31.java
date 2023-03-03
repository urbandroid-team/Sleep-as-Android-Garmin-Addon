package com.urbandroid.sleep.garmin;

import static android.provider.Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS;

import static com.urbandroid.sleep.garmin.Utils.startAppInfo;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ForegroundServiceStartNotAllowedException;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.annotation.RequiresApi;
import androidx.core.content.ContextCompat;

@RequiresApi(api = Build.VERSION_CODES.S)
public class UtilsAPI31 {
    public static void startForegroundService(Context context, Intent serviceIntent) {
        try {
            ContextCompat.startForegroundService(context, serviceIntent);
        } catch (ForegroundServiceStartNotAllowedException e) {
            Utils.showUnrestrictedBatteryNeededNotificationIfNeeded(context);
        }
    }
}
