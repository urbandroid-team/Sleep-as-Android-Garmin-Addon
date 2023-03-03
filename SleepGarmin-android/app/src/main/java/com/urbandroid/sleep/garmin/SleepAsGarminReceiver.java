package com.urbandroid.sleep.garmin;

import static com.urbandroid.sleep.garmin.Constants.CHECK_CONNECTED;
import static com.urbandroid.sleep.garmin.Constants.DO_HR_MONITORING;
import static com.urbandroid.sleep.garmin.Constants.HINT;
import static com.urbandroid.sleep.garmin.Constants.IQ_APP_ID;
import static com.urbandroid.sleep.garmin.Constants.PACKAGE_SLEEP;
import static com.urbandroid.sleep.garmin.Constants.REPORT;
import static com.urbandroid.sleep.garmin.Constants.SET_BATCH_SIZE;
import static com.urbandroid.sleep.garmin.Constants.SET_PAUSE;
import static com.urbandroid.sleep.garmin.Constants.STARTED_ON_WATCH_NAME;
import static com.urbandroid.sleep.garmin.Constants.START_ALARM;
import static com.urbandroid.sleep.garmin.Constants.START_WATCH_APP;
import static com.urbandroid.sleep.garmin.Constants.STOP_ALARM;
import static com.urbandroid.sleep.garmin.Constants.STOP_WATCH_APP;
import static com.urbandroid.sleep.garmin.Constants.UPDATE_ALARM;
import static com.urbandroid.sleep.garmin.GlobalInitializer.debug;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.widget.Toast;

import androidx.core.content.ContextCompat;

import com.garmin.android.connectiq.ConnectIQ;
import com.garmin.android.connectiq.IQDevice;
import com.urbandroid.common.error.ErrorReporter;
import com.urbandroid.common.logging.Logger;

/**
 * Created by artaud on 29.12.16.
 */

public class SleepAsGarminReceiver extends BroadcastReceiver {

    private static final String TAG = SleepAsGarminReceiver.class.getSimpleName();
    private boolean sleepInstalled = true;

    @Override
    public void onReceive(Context context, Intent intent) {
        GlobalInitializer.initializeIfRequired(context);
        Logger.logInfo(TAG + " onReceive: " + intent.getAction());

        checkSleepInstalled(context);

        try {
            if (ConnectIQ.INCOMING_MESSAGE.equals(intent.getAction()) && !SleepAsAndroidProviderService.RUNNING) {

                if (debug) {
                    IQDevice device = intent.getParcelableExtra(ConnectIQ.EXTRA_REMOTE_DEVICE);
                    if (intent.hasExtra(ConnectIQ.EXTRA_REMOTE_DEVICE) && device.getFriendlyName().equals("Simulator")) {
                        startCommServicesBecauseWatchSaidSo(context);
                    }

                } else if (intent.hasExtra(ConnectIQ.EXTRA_APPLICATION_ID) && IQ_APP_ID.equals(intent.getStringExtra(ConnectIQ.EXTRA_APPLICATION_ID)) &&
                        !SleepAsAndroidProviderService.RUNNING) {
                    startCommServicesBecauseWatchSaidSo(context);
                }
            }
        } catch (IllegalArgumentException e) {
            Logger.logInfo(TAG, e);
        }

        Logger.logInfo("Receiver intent: " + intent.getAction().toString());

        String action = intent.getAction() != null ? intent.getAction() : "";
        Boolean serviceRunning = SleepAsAndroidProviderService.RUNNING;

        switch (action) {
            case START_WATCH_APP: {
                Intent serviceIntent = new Intent(context, SleepAsAndroidProviderService.class);
                serviceIntent.setAction(START_WATCH_APP);
                if (intent.hasExtra(DO_HR_MONITORING)) {
                    serviceIntent.putExtra("DO_HR_MONITORING", true);
                }
                Utils.startForegroundService(context, serviceIntent);
                break;
            }
            case STOP_WATCH_APP:
                Logger.logInfo("Received stop watch app, service running? " + serviceRunning);
                if (serviceRunning) {
                    Intent serviceIntent = new Intent(context, SleepAsAndroidProviderService.class);
                    serviceIntent.setAction(STOP_WATCH_APP);
                    Utils.startForegroundService(context, serviceIntent);
                }
                break;
            case SET_PAUSE:
                if (serviceRunning) {
                    Intent serviceIntent = new Intent(context, SleepAsAndroidProviderService.class);
                    serviceIntent.setAction(SET_PAUSE);
                    serviceIntent.putExtra("TIMESTAMP", intent.getLongExtra("TIMESTAMP", 0));
                    Utils.startForegroundService(context, serviceIntent);
                }
                break;
            case SET_BATCH_SIZE:
                if (serviceRunning) {
                    Intent serviceIntent = new Intent(context, SleepAsAndroidProviderService.class);
                    serviceIntent.setAction(SET_BATCH_SIZE);
                    serviceIntent.putExtra("SIZE", intent.getLongExtra("SIZE", 0));
                    Utils.startForegroundService(context,serviceIntent);
                }

                break;
            case HINT:
                if (serviceRunning) {
                    Intent serviceIntent = new Intent(context, SleepAsAndroidProviderService.class);
                    serviceIntent.setAction(HINT);
                    serviceIntent.putExtra("REPEAT", Utils.getLongOrIntExtraAsLong(intent, "REPEAT", 0L));
                    Utils.startForegroundService(context, serviceIntent);
                }
                break;
            case UPDATE_ALARM:
                if (serviceRunning) {
                    Intent serviceIntent = new Intent(context, SleepAsAndroidProviderService.class);
                    serviceIntent.setAction(UPDATE_ALARM);
                    serviceIntent.putExtra("TIMESTAMP", intent.getLongExtra("TIMESTAMP", 0));
                    Utils.startForegroundService(context, serviceIntent);
                }
                break;
            case START_ALARM:
                if (serviceRunning) {
                    Intent serviceIntent = new Intent(context, SleepAsAndroidProviderService.class);
                    serviceIntent.putExtra("DELAY", intent.getIntExtra("DELAY", 0));
                    serviceIntent.setAction(START_ALARM);
                    Utils.startForegroundService(context, serviceIntent);
                }
                break;
            case STOP_ALARM:
                if (serviceRunning) {
                    Intent serviceIntent = new Intent(context, SleepAsAndroidProviderService.class);
                    serviceIntent.setAction(STOP_ALARM);
                    Utils.startForegroundService(context, serviceIntent);
                }
                break;
            case CHECK_CONNECTED: {
                Logger.logDebug("Receiver: Check connected");
                Intent serviceIntent = new Intent(context, SleepAsAndroidProviderService.class);
                serviceIntent.setAction(CHECK_CONNECTED);
                Utils.startForegroundService(context, serviceIntent);
                break;
            }
            case REPORT:
                Logger.logInfo("Generating on demand report");
                Logger.logInfo(context.getPackageName());

                String comment = "No comment";
                if (intent.hasExtra("USER_COMMENT")) {
                    comment = intent.getStringExtra("USER_COMMENT");
                }
                ErrorReporter.getInstance().generateOnDemandReport(null, "Manual report", comment);
                break;
        }
    }

    private void checkSleepInstalled(Context context) {
        try {
            context.getPackageManager().getApplicationInfo(PACKAGE_SLEEP, 0);
        } catch (PackageManager.NameNotFoundException e) {
            Logger.logInfo(TAG + "Sleep not installed");
            sleepInstalled = false;
        }

        if (!sleepInstalled) {
            Toast.makeText(context, R.string.install_saa, Toast.LENGTH_LONG).show();
            try {
                Intent goToMarket = new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + PACKAGE_SLEEP));
                context.startActivity(goToMarket);
            } catch (Exception e) {
                Logger.logInfo(TAG, e);
            }
        }
    }

    private void startCommServicesBecauseWatchSaidSo(Context context) {
        startProviderServiceBecauseWatchSaidSo(context);

        Intent startIntent = new Intent(STARTED_ON_WATCH_NAME);
        startIntent.setPackage(PACKAGE_SLEEP);
        context.sendBroadcast(startIntent);
    }


    private void startProviderServiceBecauseWatchSaidSo(Context context) {
        Logger.logInfo(TAG + " ConnectIQ intent received, starting provider service...");
        Utils.startForegroundService(context,new Intent(context, SleepAsAndroidProviderService.class));
    }
}
