package com.urbandroid.sleep.garmin;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;

public class Notifications {

    final static String NOTIFICATION_CHANNEL_ID_TRACKING = "garminTrackingChannel";

    public static void createChannels(Context context) {
        if (Build.VERSION.SDK_INT >= 26) {

            NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

            CharSequence channelName = context.getResources().getString(R.string.running);

            int importance = NotificationManager.IMPORTANCE_MIN;
            NotificationChannel notificationChannel = new NotificationChannel(NOTIFICATION_CHANNEL_ID_TRACKING, channelName, importance);
            notificationManager.createNotificationChannel(notificationChannel);
        }
    }
}
