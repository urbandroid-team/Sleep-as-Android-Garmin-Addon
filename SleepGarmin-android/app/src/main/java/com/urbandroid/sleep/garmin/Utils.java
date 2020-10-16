package com.urbandroid.sleep.garmin;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;

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

}
