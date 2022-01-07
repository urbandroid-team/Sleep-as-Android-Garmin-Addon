package com.urbandroid.sleep.garmin;

import static com.urbandroid.sleep.garmin.Constants.ACTION_STOP_SELF;
import static com.urbandroid.sleep.garmin.Notifications.NOTIFICATION_CHANNEL_ID_TRACKING;

import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.urbandroid.common.logging.Logger;

import java.io.IOException;

public class HttpServerService extends Service {
    private static final String TAG = "HttpServerService";
    private static final int PORT_DEFAULT = 1765;

    private boolean running = false;
    private HttpServer server;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        if (!running) {
            running = true;
            server = new HttpServer(PORT_DEFAULT, this);
            try {
                server.start();
            } catch (IOException e) {
                e.printStackTrace();
                Logger.logSevere(TAG + ": IOException when starting HttpServer", e);
            }
            Handler h = new Handler();
        }

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        server.stop();
    }

    @Override
    public int onStartCommand(final Intent intent, int flags, int startId) {
        Logger.logDebug(TAG + "onStartCommand, intent " + ((intent != null && intent.getAction() != null) ? intent.getAction() : "null"));

        startForeground();
        running = true;

//        ciqManager.init(this, intent);

        return START_STICKY;
    }

    private void startForeground() {
        final Intent stopIntent = new Intent(this, SleepAsAndroidProviderService.class);
        stopIntent.setAction(ACTION_STOP_SELF);

        PendingIntent pendingIntent = PendingIntent.getService(this, 151, stopIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        if (Build.VERSION.SDK_INT >= 26) {
            pendingIntent = PendingIntent.getForegroundService(this, 151, stopIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        }

        final NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID_TRACKING)
                .setContentIntent(pendingIntent)
                .setColor(getResources().getColor(R.color.tint_dark))
                .addAction(R.drawable.ic_action_stop, getResources().getString(R.string.stop), pendingIntent)
                .setContentText(getString(R.string.running_server));

        if (Build.VERSION.SDK_INT < 24) {
            notificationBuilder.setContentTitle(getResources().getString(R.string.app_name_long));
        }

        notificationBuilder.setSmallIcon(R.drawable.ic_action_track);

        startForeground(1350, notificationBuilder.build());
    }


}
