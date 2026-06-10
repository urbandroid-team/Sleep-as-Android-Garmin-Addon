package com.urbandroid.sleep.garmin;

import static com.urbandroid.sleep.garmin.Constants.PACKAGE_GCM;
import static com.urbandroid.sleep.garmin.Constants.PACKAGE_SLEEP;
import static com.urbandroid.sleep.garmin.Constants.PACKAGE_SLEEP_WATCH_STARTER;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import com.urbandroid.common.logging.Logger;

public class MainActivity extends Activity {

    private static final String TAG = MainActivity.class.getSimpleName();

    private boolean sleepInstalled = true;
    private boolean gcmInstalled = true;
    private boolean watchappInstalled = true;
    private boolean watchsleepstarterInstalled = true;
    private static final int PERMISSION_POST_NOTIFICATIONS_REQUEST_CODE = 420;

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Logger.logDebug("Main Activity connectIQ intialization");


        findViewById(R.id.install_gcm).setOnClickListener(v -> installGCM());

        findViewById(R.id.install_saa).setOnClickListener(v -> installSleep());

        findViewById(R.id.setup).setOnClickListener(v -> setupSleep());
        findViewById(R.id.install_watchsleepstarter).setOnClickListener(v -> installSleepWatchStarter());

    }




    @Override
    protected void onResume() {
        super.onResume();

        try {
            this.getPackageManager().getApplicationInfo(PACKAGE_SLEEP, 0);
        } catch (PackageManager.NameNotFoundException e) {
            sleepInstalled = false;
        }

        try {
            this.getPackageManager().getApplicationInfo(PACKAGE_GCM, 0);
        } catch (PackageManager.NameNotFoundException e) {
            gcmInstalled = false;
        }

        try {
            this.getPackageManager().getApplicationInfo(PACKAGE_SLEEP_WATCH_STARTER, 0);
        } catch (PackageManager.NameNotFoundException e) {
            watchsleepstarterInstalled = false;
        }

        findViewById(R.id.card_install_saa).setVisibility(!sleepInstalled ? View.VISIBLE : View.GONE);
        findViewById(R.id.card_install_gcm).setVisibility(!gcmInstalled ? View.VISIBLE : View.GONE);
        findViewById(R.id.card_install_watchsleepstarter).setVisibility(!watchsleepstarterInstalled ? View.VISIBLE : View.GONE);

    }

    private void setupSleep() {
            try {
                Intent i = new Intent();
                i.setClassName(PACKAGE_SLEEP, PACKAGE_SLEEP+".alarmclock.settings.SmartwatchSettingsActivity");
                startActivity(i);
            } catch (Exception e) {
                try {
                    Intent i = new Intent();
                    i.setClassName(PACKAGE_SLEEP, PACKAGE_SLEEP+".alarmclock.AlarmClock");
                    startActivity(i);
                } catch (Exception ee) {
                    Log.e(TAG, "Error", ee);
                    finish();
                }
            }
        }

    private void installSleep() {
        try {
            Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id="+PACKAGE_SLEEP));
            startActivity(i);
        } catch (Exception e) {
            Log.e(TAG, "Error", e);
        }
    }

    private void installGCM() {
        try {
            Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id="+PACKAGE_GCM));
            startActivity(i);
        } catch (Exception e) {
            Log.e(TAG, "Error", e);
        }
    }

    private void installSleepWatchStarter(){
        try {
            Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id="+PACKAGE_SLEEP_WATCH_STARTER));
            startActivity(i);
        } catch (Exception e) {
            Log.e(TAG, "Error", e);
        }
    }
}
