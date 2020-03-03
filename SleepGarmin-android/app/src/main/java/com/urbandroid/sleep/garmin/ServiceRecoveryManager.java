package com.urbandroid.sleep.garmin;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.support.v4.app.AlarmManagerCompat;

import com.urbandroid.common.logging.Logger;

import java.util.Date;
import java.util.concurrent.TimeUnit;

import static com.urbandroid.sleep.garmin.Constants.ACTION_RESTART_SELF;

public class ServiceRecoveryManager {

    private static final ServiceRecoveryManager ourInstance = new ServiceRecoveryManager();
    private static final String TAG = "ServiceRecoveryManager: ";
    private final static long TIME_TO_RECOVER = TimeUnit.MINUTES.toMillis(15);

    public static ServiceRecoveryManager getInstance() {
        return ourInstance;
    }

    private Service service;

    private ServiceRecoveryManager() {
    }

    public void init(Service service) {
        this.service = service;
    }

    public void stopSelfAndDontScheduleRecovery(String reason) { stopSelfAndDontScheduleRecovery(service, reason); }

    public void stopSelfAndScheduleRecovery(String reason) { stopSelfAndScheduleRecovery(service, reason); }

    private void stopSelfAndDontScheduleRecovery(Service service, String reason) {
        Logger.logDebug(TAG + "stopSelfAndDontScheduleRecovery, reason: " + reason);
        QueueToWatch.getInstance().emptyQueue();
        cancelRecovery(service);
        service.stopSelf();
    }

    private void stopSelfAndScheduleRecovery(Service service, String reason) {
        Logger.logDebug(TAG + "stopSelfAndScheduleRecovery, reason: " + reason);
        service.stopSelf();

        PendingIntent pi = getRecoveryIntent(service);

        final AlarmManager m = (AlarmManager) service.getSystemService(Context.ALARM_SERVICE);
        m.cancel(pi);

        long alarmTime = System.currentTimeMillis() + TIME_TO_RECOVER;

        if (Build.VERSION.SDK_INT >= 21) {
            AlarmManagerCompat.setExactAndAllowWhileIdle(m, AlarmManager.RTC_WAKEUP, alarmTime, pi);
        } else if (Build.VERSION.SDK_INT >= 19) {
            m.setExact(AlarmManager.RTC_WAKEUP, alarmTime, pi);
        } else {
            m.set(AlarmManager.RTC_WAKEUP, alarmTime, pi);
        }

        Logger.logInfo("Restart alarm scheduled " + new Date(alarmTime));
    }

    private PendingIntent getRecoveryIntent(Service service) {
        Intent pendingIntent = new Intent(service, SleepAsAndroidProviderService.class);
        pendingIntent.setAction(ACTION_RESTART_SELF);
        pendingIntent.setPackage(service.getPackageName());

        PendingIntent pi = PendingIntent.getService(service.getApplicationContext(), 0, pendingIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        if (Build.VERSION.SDK_INT >= 26) {
            pi = PendingIntent.getForegroundService(service.getApplicationContext(), 0, pendingIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        }

        return pi;
    }

    private void cancelRecovery(Service service) {
        final AlarmManager m = (AlarmManager) service.getSystemService(Context.ALARM_SERVICE);
        Logger.logDebug(TAG + "Canceling restart intent");
        m.cancel(getRecoveryIntent(service));
    }

}
