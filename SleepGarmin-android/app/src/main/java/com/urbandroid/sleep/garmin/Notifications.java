package com.urbandroid.sleep.garmin;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;

import static com.urbandroid.sleep.garmin.Constants.PACKAGE_SLEEP_WATCH_STARTER;

public class Notifications {

    final static String NOTIFICATION_CHANNEL_ID_TRACKING = "garminTrackingChannel";
    final static String NOTIFICATION_CHANNEL_ID_WARNING = "garminWarningChannel";

    public static void createChannels(Context context) {
        if (Build.VERSION.SDK_INT >= 26) {

            NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

            CharSequence trackingChannelTitle = context.getResources().getString(R.string.running);
            NotificationChannel trackingNotificationChannel = new NotificationChannel(NOTIFICATION_CHANNEL_ID_TRACKING, trackingChannelTitle, NotificationManager.IMPORTANCE_MIN);
            notificationManager.createNotificationChannel(trackingNotificationChannel);

            CharSequence warningChannelTitle = context.getResources().getString(R.string.running);
            NotificationChannel warningNotificationChannel = new NotificationChannel(NOTIFICATION_CHANNEL_ID_WARNING, warningChannelTitle, NotificationManager.IMPORTANCE_DEFAULT);
            notificationManager.createNotificationChannel(warningNotificationChannel);
        }
    }

    public static void showNotificationToInstallSleepWatchStarter(Context c) {
        Intent installIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id="+PACKAGE_SLEEP_WATCH_STARTER));

        installIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        PendingIntent pendingIntent = PendingIntent.getActivity(c, 0, installIntent, 0);

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(c, NOTIFICATION_CHANNEL_ID_WARNING)
                .setSmallIcon(R.drawable.ic_action_watch)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(c.getString(R.string.install_watchsleepstarter)))
                .setContentText(c.getString(R.string.install_watchsleepstarter))
                .setColor(c.getResources().getColor(R.color.tint_dark))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setOnlyAlertOnce(true)
                .setAutoCancel(true);

        if (Build.VERSION.SDK_INT < 24) {
            notificationBuilder.setContentTitle(c.getResources().getString(R.string.app_name_long));
        }

        NotificationManagerCompat nM = NotificationManagerCompat.from(c);
        nM.notify(1348, notificationBuilder.build());
    }

}
