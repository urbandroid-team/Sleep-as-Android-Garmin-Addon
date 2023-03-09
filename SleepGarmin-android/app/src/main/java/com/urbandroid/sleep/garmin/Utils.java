package com.urbandroid.sleep.garmin;

import static android.content.Context.NOTIFICATION_SERVICE;
import static android.content.Context.POWER_SERVICE;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;

import androidx.core.content.ContextCompat;

import com.urbandroid.common.logging.Logger;

import java.util.Iterator;
import java.util.Set;

public class Utils {
    public static void dumpIntent(Intent i){
        Bundle bundle = i.getExtras();
        if (bundle != null) {
            Set<String> keys = bundle.keySet();
            Iterator<String> it = keys.iterator();
            Logger.logDebug("---- Dumping Intent start");
            while (it.hasNext()) {
                String key = it.next();
                Logger.logDebug("[" + key + "=" + bundle.get(key)+"]");
            }
            Logger.logDebug("---- Dumping Intent end");
        }
    }

    public static boolean isAppInstalled(String appPackageName, Context context) {
        PackageManager pm = context.getPackageManager();
        try {
            pm.getPackageInfo(appPackageName, PackageManager.GET_ACTIVITIES);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            Logger.logDebug("Not installed: " + appPackageName);
        } catch (Exception e) {
            Logger.logDebug("IsAppInstalled", e);
            return false;
        }
        return false;
    }

    public static float[] stringArrayToFloatArray(String[] ar) {
        float[] floatAr = new float[ar.length];

        for (int i = 0; i < ar.length; i++) {
            String maxRawValue = ar[i];

            try {
                floatAr[i] = Float.parseFloat(maxRawValue);
            } catch (NumberFormatException e) {
                floatAr[i] = 0;
            }
        }

        return floatAr;
    }

    public static Long getLongOrIntExtraAsLong(Intent intent, String key, Long defaultValue) {
        Bundle extras = intent.getExtras();
        if (extras != null && extras.containsKey(key)) {
            Object extraVal = extras.get(key);
            if (extraVal == null) return defaultValue;

            if (extraVal instanceof Integer) {
                Integer intExtra = (Integer) extraVal;
                return intExtra.longValue();
            } else if (extraVal instanceof Long) {
                return (long) extraVal;
            }
        }
        return defaultValue;
    }

    public static void startForegroundService(Context context, Intent serviceIntent) {
        Logger.logDebug("Utils.startForegroundService");
        if (Build.VERSION.SDK_INT >= 31) {
            UtilsAPI31.startForegroundService(context, serviceIntent);
        } else {
            Logger.logDebug("Utils.startForegroundService alert");
            ContextCompat.startForegroundService(context, serviceIntent);
        }

    }

    public static void startAppInfo(final Activity context) {
        if (context == null) return;

        final Intent i = new Intent();
        i.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        i.addCategory(Intent.CATEGORY_DEFAULT);
        i.setData(Uri.parse("package:" + context.getPackageName()));
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        i.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
        i.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
        context.startActivity(i);
    }

    public static void showUnrestrictedBatteryDialog(Activity context) {
        new AlertDialog.Builder(context)
            .setTitle("Opt out of battery optimizations")
            .setMessage("Garmin addon for Sleep needs unrestricted battery usage on Android 12+ to be able to start." +
                    "\n\n Tap OK and in the next screen: \n\n1. Select 'App battery usage'. \n2. Tap 'Unrestricted'")
            .setPositiveButton("OK", (dialogInterface, i) -> {
                startAppInfo(context);
            })
            .show();
    }

    public static void showUnrestrictedBatteryNeededNotificationIfNeeded(Context context) {
        if (isUnrestrictedBatteryNotificationNeeded(context)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Notifications.showUnrestrictedBatteryNeededNotification(context);
            }
        }
    }

    public static boolean isUnrestrictedBatteryNotificationNeeded(Context context) {
        if (Build.VERSION.SDK_INT >= 31) {
            NotificationManager nm = (NotificationManager) context.getSystemService(NOTIFICATION_SERVICE);
            if (!nm.areNotificationsEnabled()) return false;

            PowerManager pm = (PowerManager) context.getSystemService(POWER_SERVICE);
            return pm != null && !pm.isIgnoringBatteryOptimizations(context.getPackageName());
        }
        return false;
    }
}
