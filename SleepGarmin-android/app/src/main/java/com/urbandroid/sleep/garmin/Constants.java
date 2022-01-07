package com.urbandroid.sleep.garmin;

public class Constants {

    public static final String IQ_STORE_ID = "e80a4793-f5a3-44c7-bd7f-52a97f5d8310";
    public static final String IQ_APP_ID = "21CAD9617B914811B0B27EA6240DE29B";

    // To watch
    public final static String TO_WATCH_STOP = "StopApp";
    public final static String TO_WATCH_PAUSE = "Pause";
    public final static String TO_WATCH_BATCH_SIZE = "BatchSize";
    public final static String TO_WATCH_ALARM_START = "StartAlarm";
    public final static String TO_WATCH_ALARM_STOP = "StopAlarm";
    public final static String TO_WATCH_ALARM_SET = "SetAlarm";
    public final static String TO_WATCH_HINT = "Hint";
    public final static String TO_WATCH_TRACKING_START_HR = "StartHRTracking";
    public final static String TO_WATCH_TRACKING_START = "StartTracking";

    // From watch to plugin
    public final static String NEW_DATA_ACTION_NAME = "com.urbandroid.sleep.watch.DATA_UPDATE";
    public final static String NEW_HR_DATA_ACTION_NAME = "com.urbandroid.sleep.watch.HR_DATA_UPDATE";
    public final static String PAUSE_ACTION_NAME = "com.urbandroid.sleep.watch.PAUSE_FROM_WATCH";
    public final static String RESUME_ACTION_NAME = "com.urbandroid.sleep.watch.RESUME_FROM_WATCH";
    public final static String SNOOZE_ACTION_NAME = "com.urbandroid.sleep.watch.SNOOZE_FROM_WATCH";
    public final static String DISMISS_ACTION_NAME = "com.urbandroid.sleep.watch.DISMISS_FROM_WATCH";
    public final static String STARTED_ON_WATCH_NAME = "com.urbandroid.sleep.watch.STARTED_ON_WATCH";
    public final static String STOP_SLEEP_TRACK_ACTION = "com.urbandroid.sleep.alarmclock.STOP_SLEEP_TRACK";
//    private final static String WATCH_TYPE_EXTRA = "com.urbandroid.sleep.watch.WATCH_TIME_EXTRA";
    public final static String DATA_WITH_EXTRA = "com.urbandroid.sleep.ACTION_EXTRA_DATA_UPDATE";
    public final static String EXTRA_DATA_TIMESTAMP = "com.urbandroid.sleep.EXTRA_DATA_TIMESTAMP";
    public final static String EXTRA_DATA_FRAMERATE = "com.urbandroid.sleep.EXTRA_DATA_FRAMERATE";
    public final static String EXTRA_DATA_BATCH = "com.urbandroid.sleep.EXTRA_DATA_BATCH";
    public final static String EXTRA_DATA_SPO2 = "com.urbandroid.sleep.EXTRA_DATA_SPO2"; // supposed to be float or true boolean in case of batch
    public final static String EXTRA_DATA_RR = "com.urbandroid.sleep.EXTRA_DATA_RR"; // supposed to be float or true boolean in case of batch

    //    From sleep to plugin
    final static String START_WATCH_APP = "com.urbandroid.sleep.watch.START_TRACKING";
    final static String DO_HR_MONITORING = "DO_HR_MONITORING";
    final static String STOP_WATCH_APP = "com.urbandroid.sleep.watch.STOP_TRACKING";
    final static String SET_PAUSE = "com.urbandroid.sleep.watch.SET_PAUSE";
    final static String SET_BATCH_SIZE = "com.urbandroid.sleep.watch.SET_BATCH_SIZE";
    final static String SET_SUSPENDED = "com.urbandroid.sleep.watch.SET_SUSPENDED";
    final static String START_ALARM = "com.urbandroid.sleep.watch.START_ALARM";
    final static String STOP_ALARM = "com.urbandroid.sleep.watch.STOP_ALARM";
    final static String UPDATE_ALARM = "com.urbandroid.sleep.watch.UPDATE_ALARM";
    final static String HINT = "com.urbandroid.sleep.watch.HINT";
    final static String CHECK_CONNECTED = "com.urbandroid.sleep.watch.CHECK_CONNECTED";
    final static String REPORT = "com.urbandroid.sleep.watch.REPORT";
    final static String ROMPT_NOT_SHOWN = "PROMPT_NOT_SHOWN_ON_DEVICE";

    final static String ACTION_STOP_SELF = "com.urbandroid.sleep.garmin.STOP_SELF";
    final static String ACTION_RESTART_SELF = "com.urbandroid.sleep.garmin.RESTART_SELF";

    // App Names (app names/user friendly names)
    public static final String PACKAGE_SLEEP = "com.urbandroid.sleep";
    public static final String PACKAGE_GCM = "com.garmin.android.apps.connectmobile";
    public static final String PACKAGE_SLEEP_WATCH_STARTER = "com.urbandroid.watchsleepstarter";

    //  Just for testing
    public static final String EXTRA_MESSAGE = "message";
    public static final String LOG_BROADCAST = SleepAsAndroidProviderService.class.getName() + "LogBroadcast";
    //  Testing END
}
