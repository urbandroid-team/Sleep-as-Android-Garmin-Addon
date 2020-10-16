package com.urbandroid.sleep.garmin;

import android.content.Context;
import android.content.Intent;

import com.garmin.android.connectiq.ConnectIQ;
import com.garmin.android.connectiq.IQApp;
import com.garmin.android.connectiq.IQDevice;
import com.urbandroid.common.logging.Logger;

import java.util.Arrays;
import java.util.List;

import static com.urbandroid.sleep.garmin.Constants.CHECK_CONNECTED;
import static com.urbandroid.sleep.garmin.Constants.DATA_WITH_EXTRA;
import static com.urbandroid.sleep.garmin.Constants.DISMISS_ACTION_NAME;
import static com.urbandroid.sleep.garmin.Constants.DO_HR_MONITORING;
import static com.urbandroid.sleep.garmin.Constants.EXTRA_DATA_BATCH;
import static com.urbandroid.sleep.garmin.Constants.EXTRA_DATA_RR;
import static com.urbandroid.sleep.garmin.Constants.EXTRA_DATA_SPO2;
import static com.urbandroid.sleep.garmin.Constants.EXTRA_DATA_TIMESTAMP;
import static com.urbandroid.sleep.garmin.Constants.HINT;
import static com.urbandroid.sleep.garmin.Constants.NEW_DATA_ACTION_NAME;
import static com.urbandroid.sleep.garmin.Constants.NEW_HR_DATA_ACTION_NAME;
import static com.urbandroid.sleep.garmin.Constants.PACKAGE_SLEEP;
import static com.urbandroid.sleep.garmin.Constants.PAUSE_ACTION_NAME;
import static com.urbandroid.sleep.garmin.Constants.RESUME_ACTION_NAME;
import static com.urbandroid.sleep.garmin.Constants.SET_BATCH_SIZE;
import static com.urbandroid.sleep.garmin.Constants.SET_PAUSE;
import static com.urbandroid.sleep.garmin.Constants.SNOOZE_ACTION_NAME;
import static com.urbandroid.sleep.garmin.Constants.STARTED_ON_WATCH_NAME;
import static com.urbandroid.sleep.garmin.Constants.START_ALARM;
import static com.urbandroid.sleep.garmin.Constants.START_WATCH_APP;
import static com.urbandroid.sleep.garmin.Constants.STOP_ALARM;
import static com.urbandroid.sleep.garmin.Constants.STOP_SLEEP_TRACK_ACTION;
import static com.urbandroid.sleep.garmin.Constants.STOP_WATCH_APP;
import static com.urbandroid.sleep.garmin.Constants.TO_WATCH_ALARM_SET;
import static com.urbandroid.sleep.garmin.Constants.TO_WATCH_ALARM_START;
import static com.urbandroid.sleep.garmin.Constants.TO_WATCH_ALARM_STOP;
import static com.urbandroid.sleep.garmin.Constants.TO_WATCH_BATCH_SIZE;
import static com.urbandroid.sleep.garmin.Constants.TO_WATCH_HINT;
import static com.urbandroid.sleep.garmin.Constants.TO_WATCH_PAUSE;
import static com.urbandroid.sleep.garmin.Constants.TO_WATCH_STOP;
import static com.urbandroid.sleep.garmin.Constants.TO_WATCH_TRACKING_START;
import static com.urbandroid.sleep.garmin.Constants.TO_WATCH_TRACKING_START_HR;
import static com.urbandroid.sleep.garmin.Constants.UPDATE_ALARM;
import static com.urbandroid.sleep.garmin.Utils.dumpIntent;


class MessageHandler {
    private static final MessageHandler ourInstance = new MessageHandler();
    public static MessageHandler getInstance() {
        return ourInstance;
    }

    private MessageHandler() { }

    private final static String TAG = "MessageHandler: ";

    private float[] maxFloatValues = null;
    private float[] maxRawFloatValues = null;
    private Boolean launchAppPromptAlreadyShownInCurrentSession = false;
    private long watchAppOpenTime = -1;

    private QueueToWatch queueToWatch = QueueToWatch.getInstance();

    private static void sendExplicitBroadcastToSleep(Intent intent, Context context) {
        intent.setPackage(PACKAGE_SLEEP);
        context.sendBroadcast(intent);
    }

    public void handleMessageFromWatch(List<Object> message, ConnectIQ.IQMessageStatus status, Context context) {
        Logger.logDebug(TAG + "From watch: " + message.toString() + " with status " +status.toString());
        String[] msgArray = message.toArray()[0].toString().replaceAll("\\[","").replaceAll("\\]", "").split(",");
        String receivedMsgType = msgArray[0];
        String[] receivedData = Arrays.copyOfRange(msgArray, 1, msgArray.length);

        // At this moment we are sure that app on the watch is running so we can reset the prompt latch
        launchAppPromptAlreadyShownInCurrentSession = false;

        switch (receivedMsgType) {
            case "DATA": {
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
                break;
            }
            case "DATA_NEW": {
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
                break;
            }
            case "SNOOZE":
                Intent snoozeIntent = new Intent(SNOOZE_ACTION_NAME);
                sendExplicitBroadcastToSleep(snoozeIntent, context);
                break;
            case "DISMISS":
                Intent dismissIntent = new Intent(DISMISS_ACTION_NAME);
                sendExplicitBroadcastToSleep(dismissIntent, context);
                break;
            case "PAUSE":
                Intent pauseIntent = new Intent(PAUSE_ACTION_NAME);
                sendExplicitBroadcastToSleep(pauseIntent, context);
                break;
            case "RESUME":
                Intent resumeIntent = new Intent(RESUME_ACTION_NAME);
                sendExplicitBroadcastToSleep(resumeIntent, context);
                break;
            case "STARTING":
                Intent startIntent = new Intent(STARTED_ON_WATCH_NAME);
                sendExplicitBroadcastToSleep(startIntent, context);
                break;
            case "HR":
                float[] hrData = new float[]{Float.valueOf(msgArray[1])};
                Logger.logInfo(TAG + ": received HR data from watch " + hrData[0]);
                Intent hrDataIntent = new Intent(NEW_HR_DATA_ACTION_NAME);
                hrDataIntent.putExtra("DATA", hrData);
                sendExplicitBroadcastToSleep(hrDataIntent, context);
                break;
            case "STOPPING":
                Intent stopIntent = new Intent(STOP_SLEEP_TRACK_ACTION);
                sendExplicitBroadcastToSleep(stopIntent, context);
                queueToWatch.emptyQueue();
                queueToWatch.enqueue(TO_WATCH_STOP);
                break;
            case "SPO2":
                float[] spo2 = Utils.stringArrayToFloatArray(Arrays.copyOfRange(receivedData, 0, receivedData.length - 2));
                int spo2Timestamp = Integer.parseInt(receivedData[receivedData.length - 1]);

                Logger.logInfo(TAG + ": received SpO2 data from watch " + receivedMsgType + " " + spo2Timestamp + ": " + spo2);
                Intent spo2Intent = new Intent(DATA_WITH_EXTRA);
                spo2Intent.putExtra(EXTRA_DATA_SPO2, true);
                spo2Intent.putExtra(EXTRA_DATA_TIMESTAMP, spo2Timestamp);
                spo2Intent.putExtra(EXTRA_DATA_BATCH, spo2);

                sendExplicitBroadcastToSleep(spo2Intent, context);
                break;
            case "RR":
                float[] rrData = Utils.stringArrayToFloatArray(Arrays.copyOfRange(receivedData, 0, receivedData.length - 2));
                int rrTimestamp = Integer.parseInt(receivedData[receivedData.length - 1]);

                Logger.logInfo(TAG + ": received RR data from watch " + receivedMsgType + " " + rrTimestamp + ": " + rrData);
                Intent rrDataIntent = new Intent(DATA_WITH_EXTRA);
                rrDataIntent.putExtra(EXTRA_DATA_RR, true);
                rrDataIntent.putExtra(EXTRA_DATA_TIMESTAMP, rrTimestamp);
                rrDataIntent.putExtra(EXTRA_DATA_BATCH, rrData);

                sendExplicitBroadcastToSleep(rrDataIntent, context);
                break;
        }

        if (maxRawFloatValues != null) {
            Intent dataUpdateIntent = new Intent(NEW_DATA_ACTION_NAME);
            dataUpdateIntent.putExtra("MAX_RAW_DATA", maxRawFloatValues);
            if (maxFloatValues != null) {
                dataUpdateIntent.putExtra("MAX_DATA", maxFloatValues);
            }
            sendExplicitBroadcastToSleep(dataUpdateIntent, context);
            maxRawFloatValues = null;
            maxFloatValues = null;


        }
    }

    public void handleMessageFromSleep(Intent intent, final Context context) {
        String action = intent != null ? intent.getAction() : "";
        if (action == null) {
            action = "";
        }

        if (action.equals(START_WATCH_APP)){
            Logger.logDebug(TAG + "START_WATCH_APP");
            dumpIntent(intent);

            if (intent.hasExtra(DO_HR_MONITORING)) {
                queueToWatch.enqueue(TO_WATCH_TRACKING_START_HR);
                Logger.logInfo(TAG + "TO_WATCH_TRACKING_START_HR");
            }

            queueToWatch.enqueue(TO_WATCH_TRACKING_START);
            Logger.logDebug(TAG + "TO_WATCH_TRACKING_START");
        }

        if (action.equals(STOP_WATCH_APP)) {
            Logger.logDebug(TAG + "TO_WATCH_STOP");
            queueToWatch.emptyQueue();
            queueToWatch.enqueue(TO_WATCH_STOP);
        }
        if (action.equals(SET_PAUSE)) {
            long param = intent.getLongExtra("TIMESTAMP", 0);
            Logger.logDebug(TAG + "TO_WATCH_PAUSE " + param);
            queueToWatch.enqueue(TO_WATCH_PAUSE + param);
        }
        if (action.equals(SET_BATCH_SIZE)) {
            long param = intent.getLongExtra("SIZE", 0);
            Logger.logDebug(TAG + "TO_WATCH_BATCH_SIZE " + param);
            queueToWatch.enqueue(TO_WATCH_BATCH_SIZE + param);
        }
        if (action.equals(START_ALARM)) {
            long param = intent.getIntExtra("DELAY", 0);
            Logger.logDebug(TAG + "TO_WATCH_ALARM_START, delay " + param);
            queueToWatch.enqueue(TO_WATCH_ALARM_START + param);
        }
        if (action.equals(STOP_ALARM)) {
            Logger.logDebug(TAG + "TO_WATCH_ALARM_STOP");
            queueToWatch.enqueue(TO_WATCH_ALARM_STOP);
        }
        if (action.equals(UPDATE_ALARM)) {
            long param = intent.getLongExtra("TIMESTAMP", 0);
            Logger.logDebug(TAG + "TO_WATCH_ALARM_SET " + param);
            queueToWatch.enqueue(TO_WATCH_ALARM_SET + param);
        }
        if (action.equals(HINT)) {
            long param = intent.getIntExtra("REPEAT", 0);
            Logger.logDebug(TAG + "Sending hint to watch, with repeat " + param);
            queueToWatch.enqueue(TO_WATCH_HINT + param);
        }
        if (action.equals(CHECK_CONNECTED)) {
            queueToWatch.remove(TO_WATCH_STOP);
            try {
                if (watchAppOpenTime == -1 || System.currentTimeMillis() - watchAppOpenTime >= 10000) {
                    watchAppOpenTime = System.currentTimeMillis();

                    if (!launchAppPromptAlreadyShownInCurrentSession) {
                        Logger.logDebug(TAG + "Checking watch connection...");
                        Logger.logDebug(TAG + "Setting onOpenAppOnWatch listener");
                        launchAppPromptAlreadyShownInCurrentSession = true;
                        CIQManager.getInstance().onOpenAppOnWatch(new ConnectIQ.IQOpenApplicationListener() {
                            @Override
                            public void onOpenApplicationResponse(IQDevice iqDevice, IQApp iqApp, ConnectIQ.IQOpenApplicationStatus iqOpenApplicationStatus) {
                                Logger.logDebug(TAG + "onOpenAppOnWatch response: " + iqOpenApplicationStatus);
                                if (iqOpenApplicationStatus == ConnectIQ.IQOpenApplicationStatus.PROMPT_SHOWN_ON_DEVICE) {
                                    // TODO: bug here - if nothing else comes through to the watch, the service foreground notification stays hanging, for example on alarm without tracking???
                                }

                                if (iqOpenApplicationStatus == ConnectIQ.IQOpenApplicationStatus.PROMPT_NOT_SHOWN_ON_DEVICE || iqOpenApplicationStatus == ConnectIQ.IQOpenApplicationStatus.UNKNOWN_FAILURE) {
                                    launchAppPromptAlreadyShownInCurrentSession = false;
                                }

                                if (iqOpenApplicationStatus.equals(ConnectIQ.IQOpenApplicationStatus.APP_IS_ALREADY_RUNNING)) {
                                    Intent startIntent = new Intent(STARTED_ON_WATCH_NAME);
                                    sendExplicitBroadcastToSleep(startIntent, context);
                                }
                            }
                        });
                    }
                }
            } catch (Exception e) {
                Logger.logSevere(e);
            }
        }

    }


}

