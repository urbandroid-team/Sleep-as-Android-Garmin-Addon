package com.urbandroid.sleep.garmin;

import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;
import com.garmin.android.connectiq.ConnectIQ;
import com.garmin.android.connectiq.ConnectIQ.*;
import com.garmin.android.connectiq.IQApp;
import com.garmin.android.connectiq.IQDevice;
import com.garmin.android.connectiq.exception.InvalidStateException;
import com.garmin.android.connectiq.exception.ServiceUnavailableException;
import com.urbandroid.sleep.garmin.logging.Logger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

public class SleepAsAndroidProviderService extends Service {

    public static final String IQ_APP_ID = "21CAD9617B914811B0B27EA6240DE29B";
    private static final String TAG = SleepAsAndroidProviderService.class.getSimpleName();
    public static Boolean RUNNING = false;

    private Boolean connectIqReady = false;

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

    private ConnectIQListener mListener = new ConnectIQListener() {

        @Override
        public void onInitializeError(IQSdkErrorStatus errStatus) {
            Logger.logDebug(TAG + " " + errStatus.toString());
            connectIqReady = false;
            stopSelf();
        }

        @Override
        public void onSdkReady() {
            connectIqReady = true;
            Logger.logInfo(TAG + " onSdkReady");

            registerWatchMessagesReceiver();
            checkAppIsAvailable();
        }

        @Override
        public void onSdkShutDown() {
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        GlobalInitializer.initializeIfRequired(this);
        Logger.logDebug(TAG + ": Garmin service onCreate");
        handler = new Handler();

        // checking if Garmin Connect Mobile installed
        if (isAppInstalled("com.garmin.android.apps.connectmobile")) {
            if (GlobalInitializer.debug){
                connectIQ = ConnectIQ.getInstance(this, IQConnectType.TETHERED);
            }else{
                connectIQ = ConnectIQ.getInstance(this, IQConnectType.WIRELESS);
            }

            //initialize SDK
            connectIQ.initialize(this, true, mListener);
        } else {
            final String appPackageName = "com.garmin.android.apps.connectmobile";
            Toast.makeText(getApplicationContext(), "Garmin Connect Mobile not installed", Toast.LENGTH_LONG).show();
            try {
                Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + appPackageName));
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(i);
            } catch (android.content.ActivityNotFoundException anfe) {
                Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + appPackageName));
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + appPackageName)));
            }
            stopSelf();
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
            List<IQDevice> devices = connectIQ.getKnownDevices();
            if (devices != null && devices.size() > 0) {
                Logger.logDebug( devices.get(0).toString() );
                return devices.get(0);
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
            connectIQ.getApplicationInfo(IQ_APP_ID, getDevice(ConnectIQ.getInstance()), new ConnectIQ.IQApplicationInfoListener() {

                @Override
                public void onApplicationInfoReceived(IQApp app) {
                }

                @Override
                public void onApplicationNotInstalled(String applicationId) {
                    Toast.makeText(getApplicationContext(), "Sleep not installed on watch", Toast.LENGTH_LONG).show();
                    Logger.logDebug("Sleep watch app not installed.");
                    stopSelf();
                }
            });
        } catch (InvalidStateException e) {
            Logger.logSevere(e);
        } catch (ServiceUnavailableException e) {
            Logger.logSevere(e);
        }
    }

    private void registerWatchMessagesReceiver(){
        Logger.logDebug(" registerWatchMessageRecived started");
        try {
            if (getDevice() != null) {
                connectIQ.registerForAppEvents(getDevice(), getApp(), new ConnectIQ.IQApplicationEventListener() {
                    @Override
                    public void onMessageReceived(IQDevice device, IQApp app, List<Object> message, IQMessageStatus status) {
//                 This is the place where we are intercepting messages from watch
                        Logger.logDebug("From watch: " + message.toString() + " with status " +status.toString());
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
                                    Logger.logDebug("New actigraphy [m/s2]: " + maxRawFloatValues[i]);
                                } catch (NumberFormatException e) {
                                    maxRawFloatValues[i] = 0;
                                }
                            }
                        } else if (receivedMsgType.equals("SNOOZE")) {
                            Intent snoozeIntent = new Intent(SNOOZE_ACTION_NAME);
                            sendBroadcast(snoozeIntent);
                        } else if (receivedMsgType.equals("DISMISS")) {
                            Intent dismissIntent = new Intent(DISMISS_ACTION_NAME);
                            sendBroadcast(dismissIntent);
                        } else if (receivedMsgType.equals("PAUSE")) {
                            Intent pauseIntent = new Intent(PAUSE_ACTION_NAME);
                            sendBroadcast(pauseIntent);
                        } else if (receivedMsgType.equals("RESUME")) {
                            Intent resumeIntent = new Intent(RESUME_ACTION_NAME);
                            sendBroadcast(resumeIntent);
                        } else if (receivedMsgType.equals("STARTING")) {
                            Intent startIntent = new Intent(STARTED_ON_WATCH_NAME);
                            sendBroadcast(startIntent);
                        } else if (receivedMsgType.equals("HR")) {
                            float[] hrData = new float[] {Float.valueOf(msgArray[1])};
                            Logger.logInfo(TAG + ": received HR data from watch " + hrData[0]);
                            Intent hrDataIntent = new Intent(NEW_HR_DATA_ACTION_NAME);
                            hrDataIntent.putExtra("DATA", hrData);
                            sendBroadcast(hrDataIntent);
                        } else if (receivedMsgType.equals("STOPPING")) {
                            Intent stopIntent = new Intent(STOP_SLEEP_TRACK_ACTION);
                            sendBroadcast(stopIntent);
                        }

                        if (maxFloatValues != null && maxRawFloatValues != null) {
                            Intent dataUpdateIntent = new Intent(NEW_DATA_ACTION_NAME);
                            dataUpdateIntent.putExtra("MAX_RAW_DATA", maxRawFloatValues);
                            dataUpdateIntent.putExtra("MAX_DATA", maxFloatValues);
                            sendBroadcast(dataUpdateIntent);
                            maxRawFloatValues = null;
                            maxFloatValues = null;
                        }
                    }
                });

            } else {
                Logger.logDebug(TAG + "registerWatchMessagesReceiver: No device found.");
                stopSelf();
            }
        } catch (InvalidStateException e) {
            Logger.logSevere(e);
        }
    }


    public void unregisterApp() {
        if (getDevice() != null) {
            try {
                connectIQ.unregisterForApplicationEvents(getDevice(), getApp());
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
            Logger.logDebug("Runnable sending next message NOW. SDK initialized? " + connectIqReady);
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
        Logger.logDebug("msgQueue: " + messageQueue);
        Logger.logDebug("sendNextMessage, deliveryErrorCount: " + deliveryErrorCount + " delivery in progress " + deliveryInProgress.get());
        if (deliveryErrorCount > MAX_DELIVERY_ERROR) {
            handler.removeCallbacks(sendMessageRunnable);
            emptyQueue();
            stopSelf();
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
//                Logger.logDebug("4");

                final String message = messageQueue.get(0);

                Logger.logDebug("ConnectIQ:" + connectIQ.toString());
                Logger.logDebug("Garmin app: " + getApp().getApplicationId());
                Logger.logDebug("sendNextMessage Sending message: " + message.toString());
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

    private void doSendMessage(final String message){
        Logger.logDebug("doSendMessage");
        try {
            connectIQ.sendMessage(getDevice(), getApp(), message, new IQSendMessageListener() {
                @Override
                public void onMessageStatus(IQDevice iqDevice, IQApp iqApp, IQMessageStatus status) {
    //                        Logger.logDebug("sendNextMessage Trying to send message to watch: " + message);
                    if (status != IQMessageStatus.SUCCESS) {
                        Logger.logDebug("sendNextMessage Message " + message + " failed to send to watch: " + status);
                        deliveryErrorCount++;
                    } else {
                        Logger.logDebug("sendNextMessage Successfully sent to watch: " + message + " " + status);
                        messageQueue.remove(message);
                        if (message.equals("StopApp")) {
    //                            emptyQueue();
                            stopSelf();
                        }
                        deliveryErrorCount = 0;
                    }
                    deliveryInProgress.set(false);

                    if (messageQueue.size() > 0) {
    //                            Logger.logDebug("delaying runnable post");
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
//        Logger.logDebug("Dumping extras");
        Bundle bundle = i.getExtras();
        if (bundle != null) {
            Set<String> keys = bundle.keySet();
            Iterator<String> it = keys.iterator();
//            Logger.logDebug("Dumping Intent start");
            while (it.hasNext()) {
                String key = it.next();
//                Logger.logDebug("[" + key + "=" + bundle.get(key)+"]");
            }
//            Logger.logDebug("Dumping Intent end");
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        RUNNING = true;

        String action = intent != null ? intent.getAction() : "";
        if (action == null) {
            action = "";
        }

        if (action.equals(START_WATCH_APP)) {
            Logger.logDebug("Received Start tracking command from Sleep.");
            dumpIntent(intent);
            if (intent.hasExtra(DO_HR_MONITORING)) {
                enqueue("StartHRTracking");
                Logger.logInfo("Sending do HR monitoring");
            } else {
                enqueue("StartTracking");
                Logger.logInfo("Start tracking without HR monitoring");
            }
        }
        if (action.equals(STOP_WATCH_APP)) {
            Logger.logDebug("Sending stop command to Garmin");
            emptyQueue();
            enqueue("StopApp");
        }
        if (action.equals(SET_PAUSE)) {
            long param = intent.getLongExtra("TIMESTAMP", 0);
            Logger.logDebug("Sending pause command to Garmin for " + param);
            enqueue("Pause;" + param);
        }
        if (action.equals(SET_BATCH_SIZE)) {
            long param = intent.getLongExtra("SIZE", 0);
            Logger.logDebug("Setting batch on Garmin to " + param);
            enqueue("BatchSize;" + param);
        }
        if (action.equals(START_ALARM)) {
            long param = intent.getLongExtra("DELAY", 0);
            Logger.logDebug("Sending start alarm to Garmin with delay " + param);
            enqueue("StartAlarm;" + param);
        }
        if (action.equals(STOP_ALARM)) {
            Logger.logDebug("Stopping alarm on Garmin");
            enqueue("StopAlarm;");
        }
        if (action.equals(UPDATE_ALARM)) {
            long param = intent.getLongExtra("TIMESTAMP", 0);
            Logger.logDebug("Updating Garmin alarm to " + param);
            enqueue("SetAlarm;" + param);
        }
        if (action.equals(HINT)) {
            long param = intent.getLongExtra("REPEAT", 0);
            Logger.logDebug("Sending hint to Garmin, with repeat " + param);
            enqueue("Hint;" + param);
        }
        if (action.equals(CHECK_CONNECTED)) {
            Logger.logDebug("Checking Garmin connection...");
            messageQueue.remove("StopApp");
            try {
                if (watchAppOpenTime == -1 || System.currentTimeMillis() - watchAppOpenTime >= 10000) {
                    Logger.logDebug("Trying to open app on watch...");
                    watchAppOpenTime = System.currentTimeMillis();
                    connectIQ.openApplication(getDevice(), getApp(), new IQOpenApplicationListener() {
                        @Override
                        public void onOpenApplicationResponse(IQDevice iqDevice, IQApp iqApp, IQOpenApplicationStatus iqOpenApplicationStatus) {
                        }
                    });
                }
            } catch (Exception e) {
                Logger.logSevere(e);
            }
        }
        return START_STICKY;
    }

    public void startWatchApp(){
        Logger.logDebug("Checking Garmin connection...");
        messageQueue.remove("StopApp");
        try {
            if (watchAppOpenTime == -1 || System.currentTimeMillis() - watchAppOpenTime >= 10000) {
                Logger.logDebug("Trying to open app on watch...");
                watchAppOpenTime = System.currentTimeMillis();
                connectIQ.openApplication(getDevice(), getApp(), new IQOpenApplicationListener() {
                    @Override
                    public void onOpenApplicationResponse(IQDevice iqDevice, IQApp iqApp, IQOpenApplicationStatus iqOpenApplicationStatus) {
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
        Logger.logDebug("onDestroy msgQueue: " + messageQueue);
        connectIqReady = false;
        handler.removeCallbacks(sendMessageRunnable);
        unregisterApp();

        try {
            connectIQ.shutdown(this);
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


    private boolean isAppInstalled(String appPackageName) {
        PackageManager pm = getPackageManager();
//        final String uri = "market://details?id=" + appPackageName;

        try {
            pm.getPackageInfo(appPackageName, PackageManager.GET_ACTIVITIES);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            Logger.logDebug("Not installed: " + appPackageName.toString());
        } catch (Exception e) {
            return false;
        }
        return false;
    }



}
