package com.urbandroid.sleep.garmin;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.app.AlarmManagerCompat;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.widget.Toast;

import com.garmin.android.connectiq.ConnectIQ;
import com.garmin.android.connectiq.ConnectIQ.ConnectIQListener;
import com.garmin.android.connectiq.ConnectIQ.IQConnectType;
import com.garmin.android.connectiq.ConnectIQ.IQMessageStatus;
import com.garmin.android.connectiq.ConnectIQ.IQOpenApplicationListener;
import com.garmin.android.connectiq.ConnectIQ.IQOpenApplicationStatus;
import com.garmin.android.connectiq.ConnectIQ.IQSdkErrorStatus;
import com.garmin.android.connectiq.ConnectIQ.IQSendMessageListener;
import com.garmin.android.connectiq.ConnectIQAdbStrategy;
import com.garmin.android.connectiq.IQApp;
import com.garmin.android.connectiq.IQDevice;
import com.garmin.android.connectiq.exception.InvalidStateException;
import com.garmin.android.connectiq.exception.ServiceUnavailableException;
import com.urbandroid.common.logging.Logger;

import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.urbandroid.sleep.garmin.Notifications.NOTIFICATION_CHANNEL_ID_TRACKING;

public class SleepAsAndroidProviderService extends Service {

    public static final String IQ_STORE_ID = "e80a4793-f5a3-44c7-bd7f-52a97f5d8310";
    public static final String IQ_APP_ID = "21CAD9617B914811B0B27EA6240DE29B";
    private static final String TAG = "Sleep Garmin service: ";
    public static Boolean RUNNING = false;

    private Boolean connectIqReady = false;
    private Boolean connectIqInitializing = false;

    private long watchAppOpenTime = -1;

    // From watch to plugin
    public final static String NEW_DATA_ACTION_NAME = "com.urbandroid.sleep.watch.DATA_UPDATE";
    public final static String NEW_HR_DATA_ACTION_NAME = "com.urbandroid.sleep.watch.HR_DATA_UPDATE";
    private final static String PAUSE_ACTION_NAME = "com.urbandroid.sleep.watch.PAUSE_FROM_WATCH";
    private final static String RESUME_ACTION_NAME = "com.urbandroid.sleep.watch.RESUME_FROM_WATCH";
    private final static String SNOOZE_ACTION_NAME = "com.urbandroid.sleep.watch.SNOOZE_FROM_WATCH";
    private final static String DISMISS_ACTION_NAME = "com.urbandroid.sleep.watch.DISMISS_FROM_WATCH";
    public final static String STARTED_ON_WATCH_NAME = "com.urbandroid.sleep.watch.STARTED_ON_WATCH";
    private final static String STOP_SLEEP_TRACK_ACTION = "com.urbandroid.sleep.alarmclock.STOP_SLEEP_TRACK";
//    private final static String WATCH_TYPE_EXTRA = "com.urbandroid.sleep.watch.WATCH_TIME_EXTRA";

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
    final static long TIME_TO_RECOVER = TimeUnit.MINUTES.toMillis(15);

    // App Names (app names/user friendly names)
    private static final String PACKAGE_SLEEP = "com.urbandroid.sleep";
    private static final String PACKAGE_SLEEP_USERFRIENDLY = "Sleep As Android";
    private static final String PACKAGE_GCM = "com.garmin.android.apps.connectmobile";
    private static final String PACKAGE_GCM_USERFRIENDLY = "Garmin Connect Mobile";
    private static final String PACKAGE_SLEEP_WATCH_STARTER = "com.urbandroid.watchsleepstarter";
    private static final String PACKAGE_SLEEP_WATCH_STARTER_USERFRIENDLY = "Sleep Watch Starter For Sleep";

    //  Just for testing
    public static final String EXTRA_MESSAGE = "message";
    public static final String LOG_BROADCAST = SleepAsAndroidProviderService.class.getName() + "LogBroadcast";
//  Testing END

    private ConnectIQ connectIQ;

    private int deliveryErrorCount = 0;
    private int deliveryInProgressCount = 0;
    private float[] maxFloatValues = null;
    private float[] maxRawFloatValues = null;

    public static final int MAX_DELIVERY_ERROR = 5;
    public static final int MAX_DELIVERY_IN_PROGRESS = 5;
    public static final int MESSAGE_INTERVAL = 3000;
    public static final int MESSAGE_INTERVAL_ON_FAILURE = 1000;
    public static final int MESSAGE_TIMEOUT= 20000;
//    public static final int MAX_DELIVERY_PAUSE = 20;

    private Handler handler;
    private Context context;

    @Override
    public void onCreate() {
        super.onCreate();
        GlobalInitializer.initializeIfRequired(this);
        Logger.logDebug(TAG + "Garmin service onCreate");
        handler = new Handler();

        // checking if Start sleep from watch installed
        // TODO udelat jako notfiikaci
        if (!isAppInstalled(PACKAGE_SLEEP_WATCH_STARTER) && Build.VERSION.SDK_INT >= 26) {
//            launchPlayStore(PACKAGE_SLEEP_USERFRIENDLY,PACKAGE_SLEEP_WATCH_STARTER);
        }

        connectIqInitializing = false;
        connectIqReady = false;
    }

    private void launchPlayStore(String userFriendlyName,final String appPackageName){
        // TODO line below is wrong should not be userFriendlyName as that is always Sleep
        Toast.makeText(getApplicationContext(),  userFriendlyName + " not installed", Toast.LENGTH_LONG).show();
        try {
            Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + appPackageName));
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(i);
        } catch (android.content.ActivityNotFoundException anfe) {
            Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + appPackageName));
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + appPackageName)));
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Nullable
    private IQDevice getDevice() {
        return getDevice(ConnectIQ.getInstance());
    }

    @Nullable
    private IQDevice getDevice(ConnectIQ connectIQ) {
        try {
            List<IQDevice> devices = connectIQ.getConnectedDevices();
            if (devices != null && devices.size() > 0) {
                Logger.logDebug(TAG + "getDevice connected: " + devices.get(0).toString() );
                return devices.get(0);
            } else {
                devices = connectIQ.getKnownDevices();
                if (devices != null && devices.size() > 0) {
                    Logger.logDebug(TAG + "getDevice known: " + devices.get(0).toString() );
                    return devices.get(0);
                }
            }
        } catch (InvalidStateException e) {
            Logger.logSevere(e);
        } catch (ServiceUnavailableException e) {
            Logger.logSevere(e);
        }
        return null;
    }

    private IQApp getApp() {
        return getApp(ConnectIQ.getInstance());
    }

    private IQApp getApp(ConnectIQ connectIQ) {
        return new IQApp(IQ_APP_ID);
    }

    public void checkAppIsAvailable() {
        try {
            connectIQ.getApplicationInfo(IQ_APP_ID, getDevice(connectIQ), new ConnectIQ.IQApplicationInfoListener() {

                @Override
                public void onApplicationInfoReceived(IQApp app) {

                }

                @Override
                public void onApplicationNotInstalled(String applicationId) {
                        Toast.makeText(getApplicationContext(), "Sleep not installed on your Garmin watch", Toast.LENGTH_LONG).show();
                        Logger.logDebug(TAG + "Sleep watch app not installed.");
                        stopSelfAndDontScheduleRecovery(getApplicationContext());
                }
            });
        } catch (InvalidStateException e) {
            Logger.logSevere(e);
        } catch (ServiceUnavailableException e) {
            Logger.logSevere(e);
        }
    }

    private void registerWatchMessagesReceiver(){
        Logger.logDebug(TAG + "registerWatchMessageReceiver started");
        try {
            if (getDevice(connectIQ) != null) {
                connectIQ.registerForAppEvents(getDevice(connectIQ), getApp(connectIQ), new ConnectIQ.IQApplicationEventListener() {
                    @Override
                    public void onMessageReceived(IQDevice device, IQApp app, List<Object> message, IQMessageStatus status) {
//                 This is the place where we are intercepting messages from watch
                        Logger.logDebug(TAG + "From watch: " + message.toString() + " with status " +status.toString());
                        String[] msgArray = message.toArray()[0].toString().replaceAll("\\[","").replaceAll("\\]", "").split(",");
                        String receivedMsgType = msgArray[0];

                        if (receivedMsgType.equals("DATA")) {
                            String[] values = Arrays.copyOfRange(msgArray, 1, msgArray.length);
                            maxFloatValues = new float[values.length];
                            for (int i = 0; i < values.length; i++) {
                                String maxValue = values[i];

                                try {
                                    maxFloatValues[i] = Float.valueOf(maxValue);
                                } catch (NumberFormatException e) {
                                    maxFloatValues[i] = 0;
                                }
                            }
                        } else if (receivedMsgType.equals("DATA_NEW")) {
                            String[] values = Arrays.copyOfRange(msgArray, 1, msgArray.length);
                            maxRawFloatValues = new float[values.length];
                            for (int i = 0; i < values.length; i++) {
                                String maxRawValue = values[i];

                                try {
                                    maxRawFloatValues[i] = Float.valueOf(maxRawValue) * 9.806f / 1000f;
//                                    Logger.logDebug(TAG + "New actigraphy [m/s2]: " + maxRawFloatValues[i]);
                                } catch (NumberFormatException e) {
                                    maxRawFloatValues[i] = 0;
                                }
                            }
                        } else if (receivedMsgType.equals("SNOOZE")) {
                            Intent snoozeIntent = new Intent(SNOOZE_ACTION_NAME);
                            sendExplicitBroadcastToSleep(snoozeIntent);
                        } else if (receivedMsgType.equals("DISMISS")) {
                            Intent dismissIntent = new Intent(DISMISS_ACTION_NAME);
                            sendExplicitBroadcastToSleep(dismissIntent);
                        } else if (receivedMsgType.equals("PAUSE")) {
                            Intent pauseIntent = new Intent(PAUSE_ACTION_NAME);
                            sendExplicitBroadcastToSleep(pauseIntent);
                        } else if (receivedMsgType.equals("RESUME")) {
                            Intent resumeIntent = new Intent(RESUME_ACTION_NAME);
                            sendExplicitBroadcastToSleep(resumeIntent);
                        } else if (receivedMsgType.equals("STARTING")) {
                            Intent startIntent = new Intent(STARTED_ON_WATCH_NAME);
                            sendExplicitBroadcastToSleep(startIntent);
                        } else if (receivedMsgType.equals("HR")) {
                            float[] hrData = new float[] {Float.valueOf(msgArray[1])};
                            Logger.logInfo(TAG + ": received HR data from watch " + hrData[0]);
                            Intent hrDataIntent = new Intent(NEW_HR_DATA_ACTION_NAME);
                            hrDataIntent.putExtra("DATA", hrData);
                            sendExplicitBroadcastToSleep(hrDataIntent);
                        } else if (receivedMsgType.equals("STOPPING")) {
                            Intent stopIntent = new Intent(STOP_SLEEP_TRACK_ACTION);
                            sendExplicitBroadcastToSleep(stopIntent);
                            stopSelfAndDontScheduleRecovery(context);
                        }

                        if (maxFloatValues != null && maxRawFloatValues != null) {
                            Intent dataUpdateIntent = new Intent(NEW_DATA_ACTION_NAME);
                            dataUpdateIntent.putExtra("MAX_RAW_DATA", maxRawFloatValues);
                            dataUpdateIntent.putExtra("MAX_DATA", maxFloatValues);
                            sendExplicitBroadcastToSleep(dataUpdateIntent);
                            maxRawFloatValues = null;
                            maxFloatValues = null;
                        }
                    }
                });

            } else {
                Logger.logDebug(TAG + "registerWatchMessagesReceiver: No device found.");
                stopSelfAndScheduleRecovery(getApplicationContext());
            }
        } catch (InvalidStateException e) {
            Logger.logSevere(e);
        }
    }

    public void sendExplicitBroadcastToSleep(Intent intent) {
        intent.setPackage(MainActivity.PACKAGE_SLEEP);
        sendBroadcast(intent);
    }

    public void unregisterApp(ConnectIQ connectIQ) {
        if (getDevice(connectIQ) != null) {
            try {
                connectIQ.unregisterForApplicationEvents(getDevice(connectIQ), getApp(connectIQ));
//                connectIQ.unregisterAllForEvents();
            } catch (InvalidStateException e) {
                Logger.logSevere(e);
            } catch (IllegalArgumentException e) {
                Logger.logSevere(e);
            } catch (RuntimeException e) {
                Logger.logSevere(e);
            }
        }
    }

    private List<String> messageQueue = Collections.synchronizedList(new LinkedList<String>());
    private AtomicBoolean deliveryInProgress = new AtomicBoolean(false);

    public void enqueue(final String message) {
        if (!messageQueue.contains(message)) {
            messageQueue.add(message);
            Logger.logDebug(TAG + " Added msg to phone>watch queue: " + message);
        }
        handler.removeCallbacks(sendMessageRunnable);
        handler.postDelayed(sendMessageRunnable, 1000);
        Logger.logDebug(TAG + " Phone>watch queue: " + messageQueue);
    }

    private void scheduleSendNextMessage() {
        handler.removeCallbacks(sendMessageRunnable);
        handler.post(sendMessageRunnable);
    }

    private Runnable sendMessageRunnable = new Runnable() {
        @Override
        public void run() {
            Logger.logDebug(TAG + "Runnable sending next message NOW. SDK initialized? " + connectIqReady);
            sendNextMessage();
        }
    };

    public void emptyQueue() {
        Logger.logDebug(TAG + " emptying queue, was: " + messageQueue);
        messageQueue.clear();
        deliveryInProgress.set(false);
    }

    public void sendNextMessage() {
        if (!connectIqReady) {
            handler.removeCallbacks(sendMessageRunnable);
            handler.postDelayed(sendMessageRunnable, MESSAGE_INTERVAL);
            return;
        }
        Logger.logDebug(TAG + "msgQueue: " + messageQueue);
        Logger.logDebug(TAG + "sendNextMessage, deliveryErrorCount: " + deliveryErrorCount + " delivery in progress " + deliveryInProgress.get());
        if (deliveryErrorCount > MAX_DELIVERY_ERROR) {
            handler.removeCallbacks(sendMessageRunnable);
            emptyQueue();
            stopSelfAndScheduleRecovery(this);
        } else {
//            Logger.logDebug("1");
//                Logger.logDebug("2");
                if (messageQueue.size() < 1 || deliveryInProgress.get()) {

                    if (deliveryInProgress.get()) {
                        deliveryInProgressCount++;
                        if (deliveryInProgressCount > MAX_DELIVERY_IN_PROGRESS) {
                            deliveryInProgressCount = 0;
                            deliveryInProgress.set(false);
                            handler.removeCallbacks(sendMessageRunnable);
                            handler.postDelayed(sendMessageRunnable, MESSAGE_INTERVAL);
                        }
                    }

//                    Logger.logDebug("3, msgQsize:" + messageQueue.size() + " " + deliveryInProgress.get());
                    return;
                }
//                Logger.logDebug(TAG + "4");

                final String message = messageQueue.get(0);

                Logger.logDebug(TAG + "ConnectIQ:" + connectIQ.toString());
                Logger.logDebug(TAG + "Garmin app: " + getApp(connectIQ).getApplicationId());
                Logger.logDebug(TAG + "sendNextMessage Sending message: " + message.toString());
                deliveryInProgress.set(true);

                handler.removeCallbacks(sendMessageRunnable);
                handler.postDelayed(sendMessageRunnable, MESSAGE_TIMEOUT);

                if (GlobalInitializer.debug){
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            doSendMessage(message);
                        }
                    }).start();
                }else{
                    doSendMessage(message);
                }

        }
    }

    private PendingIntent getRecoveryIntent(Context context) {
        Intent pendingIntent = new Intent(context, SleepAsAndroidProviderService.class);
        pendingIntent.setAction(ACTION_RESTART_SELF);
        pendingIntent.setPackage(context.getPackageName());

        PendingIntent pi = PendingIntent.getService(context.getApplicationContext(), 0, pendingIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        if (Build.VERSION.SDK_INT >= 26) {
            pi = PendingIntent.getForegroundService(context.getApplicationContext(), 0, pendingIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        }

        return pi;
    }

    private void cancelRecovery(Context context) {
        final AlarmManager m = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        m.cancel(getRecoveryIntent(context));
    }

    private void stopSelfAndDontScheduleRecovery(Context context) {
        Logger.logDebug(TAG + "stopSelfAndDontScheduleRecovery");
        cancelRecovery(context);
        stopSelf();
    }

    private void stopSelfAndScheduleRecovery(Context context) {
        Logger.logDebug(TAG + "stopSelfAndScheduleRecovery");
        stopSelf();

        PendingIntent pi = getRecoveryIntent(context);

        final AlarmManager m = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
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

    private void doSendMessage(final String message){
        Logger.logDebug(TAG + "doSendMessage");
        try {
            connectIQ.sendMessage(getDevice(), getApp(connectIQ), message, new IQSendMessageListener() {
                @Override
                public void onMessageStatus(IQDevice iqDevice, IQApp iqApp, IQMessageStatus status) {
//                    Logger.logDebug(TAG + "onMessageStatus: " + message + " " + iqDevice.getDeviceIdentifier() + " " + iqApp.getApplicationId());
                    if (status != IQMessageStatus.SUCCESS) {
                        Logger.logDebug(TAG + "doSendMessage MSG " + message + " failed to send to watch: " + status);
                        deliveryErrorCount++;
                    } else {
                        Logger.logDebug(TAG + "doSendMessage Success sent to watch: " + message + " " + status);
                        messageQueue.remove(message);
                        if (message.equals("StopApp")) {
    //                            emptyQueue();
                            stopSelfAndDontScheduleRecovery(getApplicationContext());
                        }
                        deliveryErrorCount = 0;
                    }
                    deliveryInProgress.set(false);

                    if (messageQueue.size() > 0) {
    //                            Logger.logDebug(TAG + "delaying runnable post");
                        handler.removeCallbacks(sendMessageRunnable);
                        handler.postDelayed(sendMessageRunnable, messageQueue.size() > 10 ? MESSAGE_INTERVAL_ON_FAILURE : MESSAGE_INTERVAL);
                    }
                }
            });
        } catch (InvalidStateException e) {
            Logger.logDebug(TAG,e);
        } catch (ServiceUnavailableException e) {
            Logger.logDebug(TAG,e);
        }
    }

    private void logMessageToScreen(String message) {
        Intent intent = new Intent(LOG_BROADCAST);
        intent.putExtra(EXTRA_MESSAGE, message);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    public static void dumpIntent(Intent i){
//        Logger.logDebug(TAG + "Dumping extras");
        Bundle bundle = i.getExtras();
        if (bundle != null) {
            Set<String> keys = bundle.keySet();
            Iterator<String> it = keys.iterator();
            Logger.logDebug(TAG + "Dumping Intent start");
            while (it.hasNext()) {
                String key = it.next();
                Logger.logDebug(TAG + "[" + key + "=" + bundle.get(key)+"]");
            }
            Logger.logDebug(TAG + "Dumping Intent end");
        }
    }

    @Override
    public int onStartCommand(final Intent intent, int flags, int startId) {
        Logger.logDebug(TAG + "onStartCommand foreground with intent " + ((intent != null && intent.getAction() != null) ? intent.getAction() : "null"));

        startForeground();
        RUNNING = true;

        if (intent != null && intent.getAction() != null && ACTION_STOP_SELF.equals(intent.getAction())) {
            stopSelfAndDontScheduleRecovery(this);
            return START_NOT_STICKY;
        }

        if (GlobalInitializer.debug){
            connectIQ = ConnectIQ.getInstance(this, IQConnectType.TETHERED);
        } else {
            connectIQ = ConnectIQ.getInstance(this, IQConnectType.WIRELESS);
        }

        if (!connectIqReady && !connectIqInitializing) {
            connectIqInitializing = true;
            // initialize SDK
            // connectIQ.initialize(this,true,connectIQSdkListener);
            // init a wrapped SDK with fix for "Cannot cast to Long" issue viz https://forums.garmin.com/forum/developers/connect-iq/connect-iq-bug-reports/158068-?p=1278464#post1278464
            context = initializeConnectIQWrapped(this, connectIQ, false, new ConnectIQListener() {

                @Override
                public void onInitializeError(IQSdkErrorStatus errStatus) {
                    Logger.logDebug(TAG + " " + errStatus.toString());
                    connectIqReady = false;
                    stopSelfAndScheduleRecovery(getApplicationContext());
                }

                @Override
                public void onSdkReady() {
                    connectIqInitializing = false;
                    connectIqReady = true;
                    Logger.logInfo(TAG + " onSdkReady");

                    registerWatchMessagesReceiver();
                    checkAppIsAvailable();

                    handleMessageFromSleep(intent);
                }

                @Override
                public void onSdkShutDown() { }
            });
        } else if (!connectIqInitializing) {
            handleMessageFromSleep(intent);
        }

        return START_STICKY;
    }

    private void handleMessageFromSleep(Intent intent) {
        String action = intent != null ? intent.getAction() : "";
        if (action == null) {
            action = "";
        }

        if (action.equals(START_WATCH_APP)){
            Logger.logDebug(TAG + "Received Start tracking command from Sleep.");
            dumpIntent(intent);

            if (intent.hasExtra(DO_HR_MONITORING)) {
                enqueue("StartHRTracking");
                Logger.logInfo(TAG + "Using HR monitoring");
            }

            enqueue("StartTracking");
            Logger.logDebug(TAG + "Sending StartTracking to watch");
        }

        if (action.equals(STOP_WATCH_APP)) {
            Logger.logDebug(TAG + "Sending stop command to watch");
            emptyQueue();
            enqueue("StopApp");
        }
        if (action.equals(SET_PAUSE)) {
            long param = intent.getLongExtra("TIMESTAMP", 0);
            Logger.logDebug(TAG + "Sending pause command to watch for " + param);
            enqueue("Pause;" + param);
        }
        if (action.equals(SET_BATCH_SIZE)) {
            long param = intent.getLongExtra("SIZE", 0);
            Logger.logDebug(TAG + "Setting batch on watch to " + param);
            enqueue("BatchSize;" + param);
        }
        if (action.equals(START_ALARM)) {
            long param = intent.getIntExtra("DELAY", 0);
            Logger.logDebug(TAG + "Sending start alarm to watch with delay " + param);
            enqueue("StartAlarm;" + param);
        }
        if (action.equals(STOP_ALARM)) {
            Logger.logDebug(TAG + "Stopping alarm on watch");
            enqueue("StopAlarm;");
        }
        if (action.equals(UPDATE_ALARM)) {
            long param = intent.getLongExtra("TIMESTAMP", 0);
            Logger.logDebug(TAG + "Updating watch alarm to " + param);
            enqueue("SetAlarm;" + param);
        }
        if (action.equals(HINT)) {
            long param = intent.getLongExtra("REPEAT", 0);
            Logger.logDebug(TAG + "Sending hint to watch, with repeat " + param);
            enqueue("Hint;" + param);
        }
        if (action.equals(CHECK_CONNECTED)) {
            Logger.logDebug(TAG + "Checking watch connection...");
            messageQueue.remove("StopApp");
            try {
                if (watchAppOpenTime == -1 || System.currentTimeMillis() - watchAppOpenTime >= 10000) {
                    Logger.logDebug(TAG + "Trying to open app on watch...");
                    watchAppOpenTime = System.currentTimeMillis();
                    connectIQ.openApplication(getDevice(), getApp(), new IQOpenApplicationListener() {
                        @Override
                        public void onOpenApplicationResponse(IQDevice iqDevice, IQApp iqApp, IQOpenApplicationStatus iqOpenApplicationStatus) {
                            Logger.logDebug(TAG + "onOpenApplication response: " + iqOpenApplicationStatus);
                            if (iqOpenApplicationStatus.equals(IQOpenApplicationStatus.APP_IS_ALREADY_RUNNING)) {
                                Intent startIntent = new Intent(STARTED_ON_WATCH_NAME);
                                sendExplicitBroadcastToSleep(startIntent);
                            }
                        }
                    });
                }
            } catch (Exception e) {
                Logger.logSevere(e);
            }
        }

    }

    public void startWatchApp(){
        Logger.logDebug(TAG + "Checking Garmin connection...");
        messageQueue.remove("StopApp");
        try {
            if (watchAppOpenTime == -1 || System.currentTimeMillis() - watchAppOpenTime >= 10000) {
                Logger.logDebug(TAG + "Trying to open app on watch...");
                watchAppOpenTime = System.currentTimeMillis();
                connectIQ.openApplication(getDevice(), getApp(), new IQOpenApplicationListener() {
                    @Override
                    public void onOpenApplicationResponse(IQDevice iqDevice, IQApp iqApp, IQOpenApplicationStatus iqOpenApplicationStatus) {
                        if (iqOpenApplicationStatus == IQOpenApplicationStatus.PROMPT_NOT_SHOWN_ON_DEVICE) {
                            Toast.makeText(getApplicationContext(), "Failed to start Watch App. Please start manually.", Toast.LENGTH_LONG).show();
                        }
                    }
                });
            }
        } catch (Exception e) {
            Logger.logSevere(e);
        }
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        Logger.logDebug(TAG + "onDestroy msgQueue: " + messageQueue);
        connectIqReady = false;
        handler.removeCallbacks(sendMessageRunnable);
        unregisterApp(connectIQ);
        cancelRecovery(getApplicationContext());

        try {
            if (context != null) {
                Logger.logDebug("Shutting down with wrapped context");
                connectIQ.shutdown(context);
            } else {
                Logger.logDebug("Shutting down without wrapped context");
                connectIQ.shutdown(this);
            }
        } catch (InvalidStateException e) {
            // This is usually because the SDK was already shut down so no worries.
            Logger.logSevere(e);
        } catch (IllegalArgumentException e) {
            Logger.logSevere(e);
        } catch (RuntimeException e) {
            Logger.logSevere(e);
        }
        RUNNING = false;
    }

    private BroadcastReceiver receiver;

    private void startForeground() {
//        Logger.logDebug(TAG + "Starting foreground");

        final Intent stopIntent = new Intent(this, SleepAsAndroidProviderService.class);
        stopIntent.setAction(ACTION_STOP_SELF);

        PendingIntent pendingIntent = PendingIntent.getService(this, 150, stopIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        if (Build.VERSION.SDK_INT >= 26) {
            pendingIntent = PendingIntent.getForegroundService(this, 150, stopIntent, PendingIntent.FLAG_UPDATE_CURRENT);
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


    private boolean isAppInstalled(String appPackageName) {
        PackageManager pm = getPackageManager();
//        final String uri = "market://details?id=" + appPackageName;

        try {
            pm.getPackageInfo(appPackageName, PackageManager.GET_ACTIVITIES);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            Logger.logDebug(TAG + "Not installed: " + appPackageName.toString());
        } catch (Exception e) {
            Logger.logDebug(TAG, e);
            return false;
        }
        return false;
    }

    private static Context initializeConnectIQWrapped(
            Context context, ConnectIQ connectIQ, boolean autoUI, ConnectIQ.ConnectIQListener listener) {
        if (connectIQ instanceof ConnectIQAdbStrategy) {
            connectIQ.initialize(context, autoUI, listener);
            return context;
        }
        Context wrappedContext = new ContextWrapper(context) {
            private HashMap<BroadcastReceiver, BroadcastReceiver> receiverToWrapper = new HashMap<>();

            @Override
            public Intent registerReceiver(final BroadcastReceiver receiver, IntentFilter filter) {
                BroadcastReceiver wrappedRecv = new IQMessageReceiverWrapper(receiver);
                synchronized (receiverToWrapper) {
                    receiverToWrapper.put(receiver, wrappedRecv);
                }
                return super.registerReceiver(wrappedRecv, filter);
            }

            @Override
            public void unregisterReceiver(BroadcastReceiver receiver) {
            // We need to unregister the wrapped receiver.
                BroadcastReceiver wrappedReceiver = null;
                synchronized (receiverToWrapper) {
                    wrappedReceiver = receiverToWrapper.get(receiver);
                    receiverToWrapper.remove(receiver);
                }
                if (wrappedReceiver != null) super.unregisterReceiver(wrappedReceiver);
            }
        };
        connectIQ.initialize(wrappedContext, autoUI, listener);
        return wrappedContext;
    }


}
