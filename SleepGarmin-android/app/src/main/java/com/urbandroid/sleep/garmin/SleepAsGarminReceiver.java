package com.urbandroid.sleep.garmin;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.widget.Toast;

import com.garmin.android.connectiq.ConnectIQ;
import com.urbandroid.sleep.garmin.logging.Logger;
import com.urbandroid.common.error.ErrorReporter;

import static com.urbandroid.sleep.garmin.SleepAsAndroidProviderService.CHECK_CONNECTED;
import static com.urbandroid.sleep.garmin.SleepAsAndroidProviderService.HINT;
import static com.urbandroid.sleep.garmin.SleepAsAndroidProviderService.SET_BATCH_SIZE;
import static com.urbandroid.sleep.garmin.SleepAsAndroidProviderService.SET_PAUSE;
import static com.urbandroid.sleep.garmin.SleepAsAndroidProviderService.START_ALARM;
import static com.urbandroid.sleep.garmin.SleepAsAndroidProviderService.START_WATCH_APP;
import static com.urbandroid.sleep.garmin.SleepAsAndroidProviderService.STOP_ALARM;
import static com.urbandroid.sleep.garmin.SleepAsAndroidProviderService.STOP_WATCH_APP;
import static com.urbandroid.sleep.garmin.SleepAsAndroidProviderService.UPDATE_ALARM;
import static com.urbandroid.sleep.garmin.SleepAsAndroidProviderService.REPORT;

/**
 * Created by artaud on 29.12.16.
 */

public class SleepAsGarminReceiver extends BroadcastReceiver {

    private static final String TAG = SleepAsGarminReceiver.class.getSimpleName();
    private static final String PACKAGE_SLEEP = "com.urbandroid.sleep";
    private static final String PACKAGE_SLEEP_GARMIN = "com.urbandroid.sleep.garmin";
//    private static final String PACKAGE_GCM = "com.garmin.android.apps.connectmobile";
    private boolean sleepInstalled = true;
//    private boolean gcmInstalled = true;

    @Override
    public void onReceive(Context context, Intent intent) {
        GlobalInitializer.initializeIfRequired(context);
        Logger.logInfo(TAG + " onReceive: " + intent.getAction());

        try {
            context.getPackageManager().getApplicationInfo(PACKAGE_SLEEP, 0);
        } catch (PackageManager.NameNotFoundException e) {
            Logger.logInfo(TAG + "Sleep not installed");
            sleepInstalled = false;
        }

//        try {
//            context.getPackageManager().getApplicationInfo(PACKAGE_GCM, 0);
//        } catch (PackageManager.NameNotFoundException e) {
//            Logger.logInfo(TAG + "GCM not installed");
//            gcmInstalled = false;
//        }

        if (!sleepInstalled) {
            Toast.makeText(context, R.string.install_saa, Toast.LENGTH_LONG).show();
            try {
                Intent goToMarket = new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + PACKAGE_SLEEP));
                context.startActivity(goToMarket);
            } catch (Exception e) {
                Logger.logInfo(TAG, e);
            }
        }
//
//        if (sleepInstalled && !gcmInstalled) {
//            Toast.makeText(context, R.string.please_install_gcm, Toast.LENGTH_LONG).show();
//            try {
//                Intent goToMarket = new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + PACKAGE_GCM));
//                context.startActivity(goToMarket);
//            } catch (Exception e) {
//                Logger.logInfo(TAG, e);
//            }
//        }

        try {
            Logger.logDebug(intent);
            if ((ConnectIQ.INCOMING_MESSAGE.equals(intent.getAction()) && intent.hasExtra(ConnectIQ.EXTRA_APPLICATION_ID) &&
                    SleepAsAndroidProviderService.IQ_APP_ID.equals(intent.getStringExtra(ConnectIQ.EXTRA_APPLICATION_ID)) &&
                            !SleepAsAndroidProviderService.RUNNING)) {
//            if ((ConnectIQ.INCOMING_MESSAGE.equals(intent.getAction()) && !SleepAsAndroidProviderService.RUNNING)) {
                Logger.logInfo(TAG + "ConnectIQ intent received, starting service...");
                context.startService(new Intent(context, SleepAsAndroidProviderService.class));
                Intent startIntent = new Intent(SleepAsAndroidProviderService.STARTED_ON_WATCH_NAME);
                startIntent.setPackage(PACKAGE_SLEEP_GARMIN);
                context.sendBroadcast(startIntent);
            }
        } catch (IllegalArgumentException e) {
            Logger.logInfo(TAG, e);
        }

        Logger.logInfo("Receiver intent: " + intent.getAction().toString());

        String action = intent.getAction() != null ? intent.getAction() : "";

        if (action.equals(START_WATCH_APP)) {
            Intent serviceIntent = new Intent(context, SleepAsAndroidProviderService.class);
            serviceIntent.setAction(SleepAsAndroidProviderService.START_WATCH_APP);
            if (intent.hasExtra(SleepAsAndroidProviderService.DO_HR_MONITORING)) { serviceIntent.putExtra("DO_HR_MONITORING", true); }
            context.startService(serviceIntent);
        } else if (action.equals(STOP_WATCH_APP)) {
            Logger.logInfo("Received stop watch app.");
            Intent serviceIntent = new Intent(context, SleepAsAndroidProviderService.class);
            serviceIntent.setAction(SleepAsAndroidProviderService.STOP_WATCH_APP);
            context.startService(serviceIntent);
        } else if (action.equals(SET_PAUSE)) {
            Intent serviceIntent = new Intent(context, SleepAsAndroidProviderService.class);
            serviceIntent.setAction(SleepAsAndroidProviderService.SET_PAUSE);
            serviceIntent.putExtra("TIMESTAMP", intent.getLongExtra("TIMESTAMP", 0));
            context.startService(serviceIntent);
        } else if (action.equals(SET_BATCH_SIZE)) {
            Logger.logInfo("Ignoring set batch size -- Garmin cannot handle that");
//   Do nothing -- the Garmin commlink cannot handle that load!!!!
//            Intent serviceIntent = new Intent(context, SleepAsAndroidProviderService.class);
//            serviceIntent.setAction(SleepAsAndroidProviderService.SET_BATCH_SIZE);
//            serviceIntent.putExtra("SIZE", intent.getLongExtra("SIZE", 0));
//            context.startService(serviceIntent);
        } else if (action.equals(HINT)) {
            Intent serviceIntent = new Intent(context, SleepAsAndroidProviderService.class);
            serviceIntent.setAction(SleepAsAndroidProviderService.HINT);
            serviceIntent.putExtra("REPEAT", intent.getLongExtra("REPEAT", 0));
            context.startService(serviceIntent);
        } else if (action.equals(UPDATE_ALARM)) {
            Intent serviceIntent = new Intent(context, SleepAsAndroidProviderService.class);
            serviceIntent.setAction(SleepAsAndroidProviderService.UPDATE_ALARM);
            serviceIntent.putExtra("TIMESTAMP", intent.getLongExtra("TIMESTAMP", 0));
            context.startService(serviceIntent);
        } else if (action.equals(START_ALARM)) {
            Intent serviceIntent = new Intent(context, SleepAsAndroidProviderService.class);
            serviceIntent.putExtra("DELAY", intent.getLongExtra("DELAY", 0));
            serviceIntent.setAction(SleepAsAndroidProviderService.START_ALARM);
            context.startService(serviceIntent);
        } else if (action.equals(STOP_ALARM)) {
            Intent serviceIntent = new Intent(context, SleepAsAndroidProviderService.class);
            serviceIntent.setAction(SleepAsAndroidProviderService.STOP_ALARM);
            context.startService(serviceIntent);
        } else if (action.equals(CHECK_CONNECTED)) {
            Logger.logDebug("Receiver: Check connected");
            Intent serviceIntent = new Intent(context, SleepAsAndroidProviderService.class);
            serviceIntent.setAction(SleepAsAndroidProviderService.CHECK_CONNECTED);
            context.startService(serviceIntent);
        } else if (action.equals(REPORT)) {
            Logger.logInfo("Generating on demand report");
            Logger.logInfo(context.getPackageName());
            ErrorReporter.getInstance().generateOnDemandReport(null, "Manual report", "No comment");
        }
    }
}
