package com.urbandroid.sleep.garmin;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;

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
}
