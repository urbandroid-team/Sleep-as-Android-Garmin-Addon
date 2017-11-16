package com.urbandroid.sleep.garmin.logging;

/**
 * Created by artaud on 16.11.17.
 */

import android.content.Context;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.text.TextUtils;

/**
 * Class that can detect whether current code is running in emulator or not.
 * Useful for testing in emulator, where you can provide different service configurations or mock implementation of yours services when running in the emulator.
 */
public class EmulatorDetector {
    private static Boolean isEmulator = Boolean.FALSE;

    /**
     * // TODO commented out to not require PHONE_STATE permission
     * Detects, whether the code is being executed in an emulator. Works from verstions 1.5 to 2.2.
     *
     * @param context Reference to context object of current application
     * @return True, if the code is being executed in an emulator.
     */
    public static boolean isEmulator(Context context) {
//        if ( isEmulator == null )
//            isEmulator = isEmulatorResolve(context);
//
        return isEmulator;
    }
}