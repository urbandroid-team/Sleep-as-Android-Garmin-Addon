package com.urbandroid.sleep.garmin;

import static com.urbandroid.sleep.garmin.Constants.PACKAGE_SLEEP;
import static com.urbandroid.sleep.garmin.Constants.PACKAGE_SLEEP_WATCH_STARTER;
import static com.urbandroid.sleep.garmin.Constants.STARTED_ON_WATCH_NAME;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.util.Log;
import android.widget.Toast;

import com.urbandroid.common.logging.Logger;

/**
 * Created by artaud on 29.12.16.
 */

public class SleepAsGarminReceiver extends BroadcastReceiver {

    private static final String TAG = SleepAsGarminReceiver.class.getSimpleName();
    private boolean sleepInstalled = true;

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.i(TAG, "Receiver " + intent);

        checkSleepInstalled(context, PACKAGE_SLEEP);
        checkSleepInstalled(context, PACKAGE_SLEEP_WATCH_STARTER);

        try {
            if ("com.garmin.android.connectiq.INCOMING_MESSAGE".equals(intent.getAction())) {
                Intent startIntent = new Intent(intent);
                startIntent.setPackage(PACKAGE_SLEEP);
                context.sendBroadcast(startIntent);
                startCommServicesBecauseWatchSaidSo(context);
            }
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "Error", e);
        }


    }

    private void checkSleepInstalled(Context context, String packageName) {
        try {
            context.getPackageManager().getApplicationInfo(packageName, 0);
        } catch (PackageManager.NameNotFoundException e) {
            Logger.logInfo(TAG + "Sleep not installed");
            sleepInstalled = false;
        }

        if (!sleepInstalled) {
            Toast.makeText(context, R.string.install_saa, Toast.LENGTH_LONG).show();
            try {
                Intent goToMarket = new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + packageName));
                context.startActivity(goToMarket);
            } catch (Exception e) {
                Logger.logInfo(TAG, e);
            }
        }
    }

    private void startCommServicesBecauseWatchSaidSo(Context context) {
        Intent startIntent = new Intent(STARTED_ON_WATCH_NAME);
        startIntent.setPackage(PACKAGE_SLEEP);
        context.sendBroadcast(startIntent);
    }


}
