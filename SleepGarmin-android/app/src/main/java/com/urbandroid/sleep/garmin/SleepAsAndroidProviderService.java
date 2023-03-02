package com.urbandroid.sleep.garmin;

import static com.urbandroid.sleep.garmin.Constants.ACTION_STOP_SELF;
import static com.urbandroid.sleep.garmin.Constants.PACKAGE_SLEEP_WATCH_STARTER;
import static com.urbandroid.sleep.garmin.Notifications.NOTIFICATION_CHANNEL_ID_TRACKING;
import static com.urbandroid.sleep.garmin.ServiceRecoveryManager.getPendingIntentFlags;

import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;

import androidx.core.app.NotificationCompat;

import com.urbandroid.common.logging.Logger;

import org.jetbrains.annotations.Nullable;

import java.io.IOException;

public class SleepAsAndroidProviderService extends Service {

    private static final String TAG = "ProviderService: ";
    public static Boolean RUNNING = false;

    private QueueToWatch queueToWatch = QueueToWatch.getInstance();
    private CIQManager ciqManager = CIQManager.getInstance();

    private boolean serverRunning = false;
    private HttpServer server;

    @Override
    public void onCreate() {
        super.onCreate();

        Logger.logDebug(TAG + "onCreate");

        GlobalInitializer.initializeIfRequired(this);

        ServiceRecoveryManager.getInstance().init(this);

        if (!Utils.isAppInstalled(PACKAGE_SLEEP_WATCH_STARTER, this) && Build.VERSION.SDK_INT >= 26) {
            Notifications.showNotificationToInstallSleepWatchStarter(this);
        }

        ciqManager.resetState();

        startHttpServer();
    }

    private void startHttpServer() {
        if (!serverRunning) {
            serverRunning = true;
            server = new HttpServer(HttpServer.PORT_DEFAULT, this);
            try {
                server.start();
            } catch (IOException e) {
                e.printStackTrace();
                Logger.logSevere(TAG + ": IOException when starting HttpServer", e);
            }
        }
    }

    @Override
    public int onStartCommand(final Intent intent, int flags, int startId) {
        Logger.logDebug(TAG + "onStartCommand, intent " + ((intent != null && intent.getAction() != null) ? intent.getAction() : "null"));

        startForeground();
        RUNNING = true;

        if (intent != null && intent.getAction() != null && ACTION_STOP_SELF.equals(intent.getAction())) {
            ServiceRecoveryManager.getInstance().stopSelfAndDontScheduleRecovery("STOP_SELF intent received");
            return START_NOT_STICKY;
        }

        ciqManager.init(this, intent);

        return START_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        queueToWatch.logQueue("onDestroy");
        queueToWatch.cleanup();

        server.stop();
        serverRunning = false;

        ciqManager.shutdown(this);
        RUNNING = false;
        super.onDestroy();
    }

    private void startForeground() {
        final Intent stopIntent = new Intent(this, SleepAsAndroidProviderService.class);
        stopIntent.setAction(ACTION_STOP_SELF);

        PendingIntent pendingIntent = PendingIntent.getService(this, 150, stopIntent, getPendingIntentFlags());

        if (Build.VERSION.SDK_INT >= 26) {
            pendingIntent = PendingIntent.getForegroundService(this, 150, stopIntent, getPendingIntentFlags());
        }

        final NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID_TRACKING)
                .setContentIntent(pendingIntent)
                .setColor(getResources().getColor(R.color.tint_dark))
                .addAction(R.drawable.ic_action_stop, getResources().getString(R.string.stop), pendingIntent)
                .setContentText(getString(R.string.running));

        if (Build.VERSION.SDK_INT < 24) {
                notificationBuilder.setContentTitle(getResources().getString(R.string.app_name_long));
        }

        notificationBuilder.setSmallIcon(R.drawable.ic_action_track);

        startForeground(1349, notificationBuilder.build());
    }

}
