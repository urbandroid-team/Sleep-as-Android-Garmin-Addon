package com.urbandroid.sleep.garmin;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.garmin.android.connectiq.IQDevice;
import com.urbandroid.common.logging.Logger;

public class IQMessageReceiverWrapper extends BroadcastReceiver {
    private final BroadcastReceiver receiver;
    private static String TAG = "IQMessageReceiverWrapper: ";

    public IQMessageReceiverWrapper(BroadcastReceiver receiver) {
        this.receiver = receiver;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Logger.logDebug(TAG + "onReceive intent " + intent.getAction());
        if ("com.garmin.android.connectiq.SEND_MESSAGE_STATUS".equals(intent.getAction())) {
            replaceIQDeviceById(intent, "com.garmin.android.connectiq.EXTRA_REMOTE_DEVICE");
        } else if ("com.garmin.android.connectiq.OPEN_APPLICATION".equals(intent.getAction())) {
            replaceIQDeviceById(intent, "com.garmin.android.connectiq.EXTRA_OPEN_APPLICATION_DEVICE");
        } else if ("com.garmin.android.connectiq.DEVICE_STATUS".equals(intent.getAction())) {
            replaceIQDeviceById(intent, "com.garmin.android.connectiq.EXTRA_REMOTE_DEVICE");
        }
        receiver.onReceive(context, intent);
    }

    private static void replaceIQDeviceById(Intent intent, String extraName) {
        try {
            IQDevice device = intent.getParcelableExtra(extraName);
            if (device != null) {
//                Logger.logDebug("Replacing " + device.describeContents() + " " + device.getFriendlyName() + " by " + device.getDeviceIdentifier() );
                intent.putExtra(extraName, device.getDeviceIdentifier());
            }
        } catch (ClassCastException e) {
            Logger.logDebug(TAG, e);
            // It's already a long, i.e. on the simulator.
        }
    }


}
