package com.urbandroid.sleep.garmin;

import android.os.Handler;

import com.garmin.android.connectiq.ConnectIQ;
import com.garmin.android.connectiq.IQApp;
import com.garmin.android.connectiq.IQDevice;
import com.garmin.android.connectiq.exception.InvalidStateException;
import com.garmin.android.connectiq.exception.ServiceUnavailableException;
import com.urbandroid.common.logging.Logger;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class QueueToWatch {
    private static final QueueToWatch ourInstance = new QueueToWatch();
    public static QueueToWatch getInstance() {
        return ourInstance;
    }

    private QueueToWatch() { }

    private static final String TAG = "QueueToWatch: ";
    private List<String> messageQueue = Collections.synchronizedList(new LinkedList<String>());

    private Handler handler = new Handler();

    private int deliveryErrorCount = 0;
    private int deliveryInProgressCount = 0;

    public static final int MAX_DELIVERY_ERROR = 5;
    public static final int MAX_DELIVERY_IN_PROGRESS = 5;
    public static final int MESSAGE_INTERVAL = 3000;
    public static final int MESSAGE_INTERVAL_ON_FAILURE = 1000;
    public static final int MESSAGE_TIMEOUT= 20000;

    private AtomicBoolean deliveryInProgress = new AtomicBoolean(false);


    public void enqueue(final String message) {
        if (!contains(message)) {
            messageQueue.add(message);
            Logger.logDebug(TAG + " Add to queue: " + message);
        }
        handler.removeCallbacks(sendMessageRunnable);
        handler.postDelayed(sendMessageRunnable, 1000);
        this.logQueue();
    }

    public void logQueue() {
        logQueue("");
    }

    public void logQueue(String logMessage) {
        if (logMessage.length() < 1) {
            Logger.logDebug(TAG + messageQueue);
        } else {
            Logger.logDebug(TAG + logMessage + " " + messageQueue);
        }
    }

    public void emptyQueue() {
        Logger.logDebug(TAG + " emptying queue, was: " + messageQueue);
        this.clear();
        deliveryInProgress.set(false);
    }

    public String next() {
        if (messageQueue.size() > 0) {
            return messageQueue.get(0);
        }
        return null;
    }

    public int size() {
        return messageQueue.size();
    }

    public Boolean contains(String message) {
        return messageQueue.contains(message);
    }

    public void remove(String message) {
        messageQueue.remove(message);
    }

    private void clear() {
        messageQueue.clear();
    }

    private Runnable sendMessageRunnable = new Runnable() {
        @Override
        public void run() {
            sendNextMessage();
        }
    };

    private void doSendMessage(final String message){
        Logger.logDebug(TAG + "doSendMessage");

        try {
            CIQManager.getInstance().sendMessageToWatch(message, new ConnectIQ.IQSendMessageListener() {
                @Override
                public void onMessageStatus(IQDevice iqDevice, IQApp iqApp, ConnectIQ.IQMessageStatus status) {
                    Logger.logDebug(TAG + "doSendMessage to watch, status " + status + ", " + message);
                    if (status != ConnectIQ.IQMessageStatus.SUCCESS) {
                        deliveryErrorCount++;
                    } else {
                        remove(message);
                        if (message.equals("StopApp")) {
                            // We won't schedule recovery if 'StopApp' cannot be delivered. We cannot do anything in this case as the user is usually present at this moment and won't be waiting for long enough for recovery. By not scheduling recovery, we prevent a persisten notification from popping up at a later time.
                            ServiceRecoveryManager.getInstance().stopSelfAndDontScheduleRecovery();
                        }
                        deliveryErrorCount = 0;
                    }
                    deliveryInProgress.set(false);

                    if (size() > 0) {
                        handler.removeCallbacks(sendMessageRunnable);
                        handler.postDelayed(sendMessageRunnable, size() > 10 ? MESSAGE_INTERVAL_ON_FAILURE : MESSAGE_INTERVAL);
                    }
                }
            });
        } catch (InvalidStateException e) {
            Logger.logDebug(TAG, e);
        } catch (ServiceUnavailableException e) {
            Logger.logDebug(TAG, e);
        }
    }

    private void sendNextMessage() {
        if (!CIQManager.getInstance().connectIqReady) {
            handler.removeCallbacks(sendMessageRunnable);
            handler.postDelayed(sendMessageRunnable, MESSAGE_INTERVAL);
            return;
        }
        QueueToWatch.getInstance().logQueue();
        Logger.logDebug(TAG + "sendNextMessage, deliveryErrorCount: " + deliveryErrorCount + " delivery in progress " + deliveryInProgress.get());
        if (deliveryErrorCount > MAX_DELIVERY_ERROR) {
            handler.removeCallbacks(sendMessageRunnable);
            if (next() != null && next().equals(Constants.TO_WATCH_STOP)) {
                ServiceRecoveryManager.getInstance().stopSelfAndDontScheduleRecovery();
            } else {
                emptyQueue();
                ServiceRecoveryManager.getInstance().stopSelfAndScheduleRecovery();
            }
        } else {
            if (size() < 1 || deliveryInProgress.get()) {

                if (deliveryInProgress.get()) {
                    deliveryInProgressCount++;
                    if (deliveryInProgressCount > MAX_DELIVERY_IN_PROGRESS) {
                        deliveryInProgressCount = 0;
                        deliveryInProgress.set(false);
                        handler.removeCallbacks(sendMessageRunnable);
                        handler.postDelayed(sendMessageRunnable, MESSAGE_INTERVAL);
                    }
                }
                return;
            }

            final String message = next();
            if (message == null) {
                return;
            }
            Logger.logDebug(TAG + "sendNextMessage: " + message.toString());
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

    // Call only when the app is finishing!
    public void cleanup() {
        handler.removeCallbacks(sendMessageRunnable);
    }

}
