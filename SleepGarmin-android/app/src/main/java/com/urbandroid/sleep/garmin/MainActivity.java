package com.urbandroid.sleep.garmin;

import static com.garmin.android.connectiq.IQApp.IQAppStatus.INSTALLED;
import static com.urbandroid.sleep.garmin.Constants.IQ_APP_ID;
import static com.urbandroid.sleep.garmin.Constants.IQ_STORE_ID;
import static com.urbandroid.sleep.garmin.Constants.PACKAGE_GCM;
import static com.urbandroid.sleep.garmin.Constants.PACKAGE_SLEEP;
import static com.urbandroid.sleep.garmin.Constants.PACKAGE_SLEEP_WATCH_STARTER;
import static com.urbandroid.sleep.garmin.Utils.startAppInfo;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.NotificationManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.View;

import androidx.core.content.ContextCompat;

import com.garmin.android.connectiq.ConnectIQ;
import com.garmin.android.connectiq.IQApp;
import com.garmin.android.connectiq.IQDevice;
import com.garmin.android.connectiq.exception.InvalidStateException;
import com.garmin.android.connectiq.exception.ServiceUnavailableException;
import com.urbandroid.common.logging.Logger;

import java.util.List;

public class MainActivity extends Activity {

    private static final String TAG = MainActivity.class.getSimpleName();

    private boolean sleepInstalled = true;
    private boolean gcmInstalled = true;
    private boolean watchappInstalled = true;
    private boolean watchsleepstarterInstalled = true;

    private ConnectIQ mConnectIQ;
    private IQDevice mDevice;

    private static final String PERMISSION_POST_NOTIFICATIONS = "android.permission.POST_NOTIFICATIONS";
    private static final int PERMISSION_POST_NOTIFICATIONS_REQUEST_CODE = 420;

    private ConnectIQ.ConnectIQListener mListener = new ConnectIQ.ConnectIQListener() {

        @Override
        public void onInitializeError(ConnectIQ.IQSdkErrorStatus errStatus) {
            Logger.logDebug( "onIntializeError: " + errStatus.toString() );
        }

        @Override
        public void onSdkReady() {

            registerDevice();

            try {
                mConnectIQ.getApplicationInfo(IQ_APP_ID, mDevice, new ConnectIQ.IQApplicationInfoListener() {
                    @Override
                    public void onApplicationInfoReceived( IQApp app ) {
                        if (app != null) {
                            if (app.getStatus() == INSTALLED) {
                                watchappInstalled = true;
    //                            if (app.getVersion() < MY_CURRENT_VERSION) {
    //                                 Prompt the user to upgrade
    //                            }
                            } else if (app.getStatus() == IQApp.IQAppStatus.NOT_INSTALLED) {
                            } else {
                                Logger.logDebug(TAG + ": Error getting watch app: " + app.getStatus());
                            }
                        }
                        Logger.logDebug(TAG + "Watchapp installed: " + watchappInstalled);
                        findViewById(R.id.card_install_watchapp).setVisibility(!watchappInstalled ? View.VISIBLE : View.GONE);
                    }
                    @Override
                    public void onApplicationNotInstalled( String applicationId ) {
                    }
                });
            } catch (InvalidStateException e) {
                Logger.logSevere(e);
            } catch (ServiceUnavailableException e) {
                Logger.logSevere(e);
            }

        }

        @Override
        public void onSdkShutDown() {
            Logger.logDebug("onSdkShutDown");
            try {
                if (mConnectIQ != null && mDevice != null) {
                    mConnectIQ.unregisterForDeviceEvents(mDevice);
                }
            } catch (InvalidStateException e) {
                Logger.logSevere(e);
            } catch (IllegalArgumentException e) {
                Logger.logSevere(e);
            } catch (RuntimeException e) {
                Logger.logSevere(e);
            }
        }
    };

    public void registerDevice() {
        try {
            List<IQDevice> devices = mConnectIQ.getConnectedDevices();
            if (devices != null && devices.size() > 0) {
                Logger.logDebug(TAG + "getDevice connected: " + devices.get(0).toString() );
                mDevice = devices.get(0);
            } else {
                devices = mConnectIQ.getKnownDevices();
                if (devices != null && devices.size() > 0) {
                    Logger.logDebug(TAG + "getDevice known: " + devices.get(0).toString());
                    mDevice = devices.get(0);
                }
            }
            if (mDevice == null) {
                return;
            }
        } catch (InvalidStateException e) {
            Logger.logSevere(e);
        } catch (ServiceUnavailableException e) {
            Logger.logSevere(e);
        }

        try {
            mConnectIQ.registerForDeviceEvents(mDevice, new ConnectIQ.IQDeviceEventListener() {

                @Override
                public void onDeviceStatusChanged(IQDevice device, IQDevice.IQDeviceStatus status) {
                    if (device != null) {
                        Logger.logInfo("Status changed for device: " + device.getDeviceIdentifier());
                    }
                    if (status != null) {
                        Logger.logInfo("Device status changed to: " + status);
                    }
                }
            });
        } catch (InvalidStateException e) {
            Logger.logSevere(e);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        try {
            if (mDevice != null) {
                mConnectIQ.unregisterForEvents(mDevice);
            }
        } catch (InvalidStateException e) {
            Logger.logSevere(e);
        } catch (IllegalArgumentException e) {
            Logger.logSevere(e);
        }
        try {
            mConnectIQ.shutdown(this);
        } catch (InvalidStateException e) {
            Logger.logDebug(TAG, "Normal shutdown of the SDK", e);
        } catch (IllegalArgumentException e) {
            Logger.logSevere(e);
        } catch (RuntimeException e) {
            Logger.logSevere(e);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        GlobalInitializer.initializeIfRequired(this);
        setContentView(R.layout.activity_main);

        Logger.logDebug("Main Activity connectIQ intialization");

        if (GlobalInitializer.debug){
            mConnectIQ = ConnectIQ.getInstance(this, ConnectIQ.IQConnectType.TETHERED);
            Logger.logDebug("ConnectIQ instance set to TETHERED");
            mConnectIQ.initialize(this, true, mListener);
        } else {
            mConnectIQ = ConnectIQ.getInstance(this, ConnectIQ.IQConnectType.WIRELESS);
            mConnectIQ.initialize(this, true, mListener);
//            mConnectIQ.initialize(this, false, new ConnectIQ.ConnectIQListener() {
//                @Override
//                public void onSdkReady() {
//                    Logger.logDebug("Yay we['re here");
//                }
//
//                @Override
//                public void onInitializeError(ConnectIQ.IQSdkErrorStatus iqSdkErrorStatus) {}
//
//                @Override
//                public void onSdkShutDown() {}
//            });
//            Logger.logDebug("ConnectIQ instance set to WIRELESS");
        }

        findViewById(R.id.whitelist_GCM).setOnClickListener(v -> {
            startAppInfo(this, PACKAGE_GCM);
        });

        findViewById(R.id.install_gcm).setOnClickListener(v -> installGCM());

        findViewById(R.id.install_saa).setOnClickListener(v -> installSleep());

        findViewById(R.id.install_watchapp).setOnClickListener(v -> installWatchApp(mConnectIQ));

        findViewById(R.id.setup).setOnClickListener(v -> setupSleep());
        findViewById(R.id.install_watchsleepstarter).setOnClickListener(v -> installSleepWatchStarter());

        findViewById(R.id.btn_allow_notifications).setOnClickListener(v -> {
            sendUnrestrictedBatteryNotificationWithPermissionCheck();
        });

        findViewById(R.id.btn_battery_optimization_opt_out).setOnClickListener(v -> {
            startAppInfo(this, getPackageName());
        });

        sendUnrestrictedBatteryNotificationWithPermissionCheck();
    }



    private void sendUnrestrictedBatteryNotificationWithPermissionCheck() {
        if (ContextCompat.checkSelfPermission(this, PERMISSION_POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            // You can use the API that requires the permission.
            if (Build.VERSION.SDK_INT >= 24) {
                NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
                if (nm.areNotificationsEnabled()) Utils.showUnrestrictedBatteryNeededNotificationIfNeeded(this);
            } else {
                Utils.showUnrestrictedBatteryNeededNotificationIfNeeded(this);
            }
        } else if (Build.VERSION.SDK_INT >= 23 && shouldShowRequestPermissionRationale(PERMISSION_POST_NOTIFICATIONS)) {
            showPostNotificationPermissionRationaleDialog();
        } else if (Build.VERSION.SDK_INT >= 23) {
            // You can directly ask for the permission.
            requestPermissions(new String[] { PERMISSION_POST_NOTIFICATIONS }, PERMISSION_POST_NOTIFICATIONS_REQUEST_CODE);
        }
    }

    public void showPostNotificationPermissionRationaleDialog() {
        if (Build.VERSION.SDK_INT < 23) return;

        new AlertDialog.Builder(this)
                .setTitle("Allow notifications")
                .setMessage("Allow Sleep as Android: Garmin addon to show you status and warning notifications to help you maintain maximum reliability for sleep tracking using your watch.")
                .setNegativeButton("No thanks", (dialogInterface, i) -> {})
                .setPositiveButton("OK", (dialogInterface, i) -> {
                    requestPermissions(new String[]{PERMISSION_POST_NOTIFICATIONS}, PERMISSION_POST_NOTIFICATIONS_REQUEST_CODE);
                })
                .show();
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

        if (ContextCompat.checkSelfPermission(this, PERMISSION_POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            findViewById(R.id.card_allow_notifications).setVisibility(View.GONE);
        } else {
            findViewById(R.id.card_allow_notifications).setVisibility(View.VISIBLE);
        }

        findViewById(R.id.card_battery_optimization_opt_out).setVisibility(Utils.isUnrestrictedBatteryNotificationNeeded(this) ? View.VISIBLE : View.GONE);
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
                    Logger.logSevere(ee);
                    finish();
                }
            }
        }

    private void installSleep() {
        try {
            Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id="+PACKAGE_SLEEP));
            startActivity(i);
        } catch (Exception e) {
            Logger.logSevere(e);
        }
    }

    private void installGCM() {
        try {
            Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id="+PACKAGE_GCM));
            startActivity(i);
        } catch (Exception e) {
            Logger.logSevere(e);
        }
    }

    private void installWatchApp(final ConnectIQ mConnectIQ) {
        try {
            mConnectIQ.openStore(new IQApp(IQ_STORE_ID).getApplicationId());
        } catch (InvalidStateException ee) {
            mConnectIQ.initialize(this, true, new ConnectIQ.ConnectIQListener() {
                @Override
                public void onSdkReady() {
                    try {
                        mConnectIQ.openStore(new IQApp(IQ_STORE_ID).getApplicationId());
                    } catch (Exception e) {
                        Logger.logSevere(e);
                    }
                }

                @Override
                public void onInitializeError(ConnectIQ.IQSdkErrorStatus iqSdkErrorStatus) {

                }

                @Override
                public void onSdkShutDown() {

                }
            });

        } catch (Exception e) {
            Logger.logSevere(e);
        }
    }

    private void installSleepWatchStarter(){
        try {
            Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id="+PACKAGE_SLEEP_WATCH_STARTER));
            startActivity(i);
        } catch (Exception e) {
            Logger.logSevere(e);
        }
    }
}
