package com.urbandroid.sleep.garmin;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.IntentFilter;
import android.widget.Toast;

import com.garmin.android.connectiq.ConnectIQ;
import com.garmin.android.connectiq.ConnectIQAdbStrategy;
import com.garmin.android.connectiq.IQApp;
import com.garmin.android.connectiq.IQDevice;
import com.garmin.android.connectiq.exception.InvalidStateException;
import com.garmin.android.connectiq.exception.ServiceUnavailableException;
import com.urbandroid.common.logging.Logger;

import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;

import static com.urbandroid.sleep.garmin.Constants.IQ_APP_ID;

public class CIQManager {
    private static final CIQManager ourInstance = new CIQManager();

    public static CIQManager getInstance() {
        return ourInstance;
    }

    private CIQManager() {
    }

    private static final String TAG = "CIQManager: ";

    public Boolean connectIqReady = false;
    private Boolean connectIqInitializing = false;

    private ConnectIQ connectIQ;
    private Context context;


    @Nullable
    public IQDevice getDevice() {
        return getDevice(connectIQ);
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

    // Should rewrite to use connectiq.getApplicationInfo() with callback (maybe wrap in RxJava)
    public IQApp getApp() {
        return new IQApp(IQ_APP_ID);
    }

    public void resetState() {
        connectIqInitializing = false;
        connectIqReady = false;
    }

    public void init(final Context c, final Intent initialIntent) {
        if (connectIqReady && !connectIqInitializing) {
            MessageHandler.getInstance().handleMessageFromSleep(initialIntent, context);
            return;
        }

        if (connectIQ == null) {
            if (GlobalInitializer.debug){
                connectIQ = ConnectIQ.getInstance(c, ConnectIQ.IQConnectType.TETHERED);
            } else {
                connectIQ = ConnectIQ.getInstance(c, ConnectIQ.IQConnectType.WIRELESS);
            }
        }

        if (!connectIqReady && !connectIqInitializing) {
            connectIqInitializing = true;
            // initialize SDK
            // init a wrapped SDK with fix for "Cannot cast to Long" issue viz https://forums.garmin.com/forum/developers/connect-iq/connect-iq-bug-reports/158068-?p=1278464#post1278464
            context = initializeConnectIQWrapped(c, connectIQ, false, new ConnectIQ.ConnectIQListener() {

                @Override
                public void onInitializeError(ConnectIQ.IQSdkErrorStatus errStatus) {
                    Logger.logDebug(TAG + " " + errStatus.toString());
                    connectIqReady = false;
                    ServiceRecoveryManager.getInstance().stopSelfAndScheduleRecovery();
                }

                @Override
                public void onSdkReady() {
                    connectIqInitializing = false;
                    connectIqReady = true;
                    Logger.logInfo(TAG + " onSdkReady");


                    registerWatchMessagesReceiver();
                    registerDeviceStatusReceiver();
                    isWatchAppAvailable();

                    MessageHandler.getInstance().handleMessageFromSleep(initialIntent, context);
                }

                @Override
                public void onSdkShutDown() {
                    connectIqInitializing = false;
                    connectIqReady = false;
                }
            });
        }
    }

    public void onOpenAppOnWatch(ConnectIQ.IQOpenApplicationListener listener) throws InvalidStateException, ServiceUnavailableException {
        connectIQ.openApplication(getDevice(), getApp(), listener);
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

    private void isWatchAppAvailable() {
        try {
            connectIQ.getApplicationInfo(IQ_APP_ID, getDevice(), new ConnectIQ.IQApplicationInfoListener() {

                @Override
                public void onApplicationInfoReceived(IQApp app) {

                }

                @Override
                public void onApplicationNotInstalled(String applicationId) {
                    if (CIQManager.getInstance().getDevice() != null) {
                        Toast.makeText(context, "Sleep not installed on your Garmin watch", Toast.LENGTH_LONG).show();
                        Logger.logDebug(TAG + "Sleep watch app not installed.");
                    }
                    ServiceRecoveryManager.getInstance().stopSelfAndDontScheduleRecovery();
                }
            });
        } catch (InvalidStateException e) {
            Logger.logSevere(e);
        } catch (ServiceUnavailableException e) {
            Logger.logSevere(e);
        }
    }

    private void registerDeviceStatusReceiver() {
        Logger.logDebug(TAG + "registerDeviceStatusReceiver");
        IQDevice device = CIQManager.getInstance().getDevice();
        try {
            if (device != null) {
                connectIQ.registerForDeviceEvents(device, new ConnectIQ.IQDeviceEventListener() {
                    @Override
                    public void onDeviceStatusChanged(IQDevice device, IQDevice.IQDeviceStatus newStatus) {
                        Logger.logDebug(TAG + "Device status changed, now " + newStatus);
                    }
                });
            }
        } catch (InvalidStateException e) {
            e.printStackTrace();
        }
    }

    private void registerWatchMessagesReceiver(){
        Logger.logDebug(TAG + "registerWatchMessageReceiver");
        IQDevice device = CIQManager.getInstance().getDevice();
        try {
            if (device != null) {
                connectIQ.registerForAppEvents(device, getApp(), new ConnectIQ.IQApplicationEventListener() {
                    @Override
                    public void onMessageReceived(IQDevice device, IQApp app, List<Object> message, ConnectIQ.IQMessageStatus status) {
                        MessageHandler.getInstance().handleMessageFromWatch(message, status, context);
                    }
                });
            } else {
                Logger.logDebug(TAG + "registerWatchMessagesReceiver: No device found.");
                ServiceRecoveryManager.getInstance().stopSelfAndScheduleRecovery();
            }
        } catch (InvalidStateException e) {
            Logger.logSevere(e);
        }
    }

    public void shutdown(Context applicationContext) {
        connectIqReady = false;
        unregisterApp(connectIQ);

        try {
            if (context != null) {
                Logger.logDebug("Shutting down with wrapped context");
                connectIQ.shutdown(context);
            } else {
                Logger.logDebug("Shutting down without wrapped context");
                connectIQ.shutdown(applicationContext);
            }
        } catch (InvalidStateException e) {
            // This is usually because the SDK was already shut down so no worries.
            Logger.logSevere(e);
        } catch (IllegalArgumentException e) {
            Logger.logSevere(e);
        } catch (RuntimeException e) {
            Logger.logSevere(e);
        }
    }

    private void unregisterApp(ConnectIQ connectIQ) {
        try {
            if (connectIQ != null) {
                IQDevice device = getDevice();
                if (device != null) {
                    connectIQ.unregisterForApplicationEvents(device, getApp());
                    connectIQ.unregisterForDeviceEvents(device);
                }
            }
        } catch (InvalidStateException e) {
            Logger.logSevere(e);
        } catch (IllegalArgumentException e) {
            Logger.logSevere(e);
        } catch (RuntimeException e) {
            Logger.logSevere(e);
        }
    }

}
