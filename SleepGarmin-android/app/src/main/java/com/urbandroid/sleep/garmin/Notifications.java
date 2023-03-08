package com.urbandroid.sleep.garmin;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;

import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import static com.urbandroid.sleep.garmin.Constants.PACKAGE_SLEEP_WATCH_STARTER;

public class Notifications {

    final static String NOTIFICATION_CHANNEL_ID_TRACKING = "garminTrackingChannel";
    final static String NOTIFICATION_CHANNEL_ID_WARNING = "garminWarningChannel";
    private static final String NOTIFICATION_CHANNEL_ID_REPORT = "garminReportChannel";


    public static void createChannels(Context context) {
        if (Build.VERSION.SDK_INT >= 26) {

            NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

            CharSequence trackingChannelTitle = context.getResources().getString(R.string.running);
            NotificationChannel trackingNotificationChannel = new NotificationChannel(NOTIFICATION_CHANNEL_ID_TRACKING, trackingChannelTitle, NotificationManager.IMPORTANCE_MIN);
            notificationManager.createNotificationChannel(trackingNotificationChannel);

            CharSequence warningChannelTitle = context.getResources().getString(R.string.running);
            NotificationChannel warningNotificationChannel = new NotificationChannel(NOTIFICATION_CHANNEL_ID_WARNING, warningChannelTitle, NotificationManager.IMPORTANCE_DEFAULT);
            notificationManager.createNotificationChannel(warningNotificationChannel);

            CharSequence reportChannelTitle = context.getResources().getString(R.string.on_demand_report_title);
            NotificationChannel reportNotificationChannel = new NotificationChannel(NOTIFICATION_CHANNEL_ID_REPORT, reportChannelTitle, NotificationManager.IMPORTANCE_HIGH);
            notificationManager.createNotificationChannel(reportNotificationChannel);
        }
    }

    public static void showNotificationToInstallSleepWatchStarter(Context c) {
        Intent installIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id="+PACKAGE_SLEEP_WATCH_STARTER));

        installIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        PendingIntent pendingIntent = PendingIntent.getActivity(c, 0, installIntent, 0);

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(c, NOTIFICATION_CHANNEL_ID_WARNING)
                .setSmallIcon(R.drawable.ic_action_watch)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(c.getString(R.string.install_sleep_watch_starter)))
                .setContentText(c.getString(R.string.install_sleep_watch_starter))
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

    public static void showCannotStartFromPhoneNotification(Context c) {
        Intent documentationIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://docs.sleep.urbandroid.org/faqs/garmin_start_dialog_bug.html"));

        documentationIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        PendingIntent pendingIntent = PendingIntent.getActivity(c, 0, documentationIntent, 0);

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(c, NOTIFICATION_CHANNEL_ID_WARNING)
                .setSmallIcon(R.drawable.ic_action_watch)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(c.getString(R.string.cannot_start_from_phone)))
                .setContentText(c.getString(R.string.cannot_start_from_phone))
                .setColor(c.getResources().getColor(R.color.tint_dark))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setOnlyAlertOnce(true)
                .setAutoCancel(true);

        if (Build.VERSION.SDK_INT < 24) {
            notificationBuilder.setContentTitle(c.getResources().getString(R.string.app_name_long));
        }

        NotificationManagerCompat nM = NotificationManagerCompat.from(c);
        nM.notify(1375, notificationBuilder.build());
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    public static void showUnrestrictedBatteryNeededNotification(Context c) {
        final Intent i = new Intent();
        i.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        i.addCategory(Intent.CATEGORY_DEFAULT);
        i.setData(Uri.parse("package:" + c.getPackageName()));
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        i.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
        i.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);

        PendingIntent pendingIntent = PendingIntent.getActivity(c, 0, i, PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(c, NOTIFICATION_CHANNEL_ID_WARNING)
                .setSmallIcon(R.drawable.ic_action_watch)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(c.getString(R.string.unrestricted_battery_needed)))
                .setContentText(c.getString(R.string.unrestricted_battery_needed))
                .setColor(c.getResources().getColor(R.color.tint_dark))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setOnlyAlertOnce(true)
                .setAutoCancel(true);

        if (Build.VERSION.SDK_INT < 24) {
            notificationBuilder.setContentTitle(c.getResources().getString(R.string.app_name_long));
        }

        NotificationManagerCompat nM = NotificationManagerCompat.from(c);
        nM.notify(1389, notificationBuilder.build());
    }

}
