package com.urbandroid.sleep.garmin.logging;

import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.os.Build;
import android.os.PowerManager;
import android.os.Process;
import android.provider.Settings;
import android.telephony.TelephonyManager;

import com.urbandroid.common.util.TimeZoneCountryMap;
import com.urbandroid.sleep.garmin.logging.*;
//import com.urbandroid.common.util.TimeZoneCountryMap;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import javax.security.auth.x500.X500Principal;

public class Environment {
    private static volatile Integer apiLevel;
    private static volatile String cpuAbi;
    private static volatile String cachedImei = null;

    private static final X500Principal DEBUG_DN = new X500Principal("CN=Android Debug,O=Android,C=US");
    private static volatile Boolean isDebugCertificate = null;

    public static boolean isHoneycombOrGreater() {
        return getAPILevel() > 10;
    }

    public static boolean isFroyoOrGreater() {
        return getAPILevel() > 7;
    }

    public static boolean isGingerOrGreater() {
        return getAPILevel() > 8;
    }

    public static boolean isGingerOrLess() {
        return getAPILevel() <= 10;
    }

    public static boolean isIcsOrGreater() {
        return getAPILevel() >= 14;
    }

    public static boolean isNewJellyBeanOrGreater() {
        return getAPILevel() > 16;
    }

    public static boolean isJellyBeanOrGreater() {
        return getAPILevel() >= 16;
    }

    public static boolean isKitKatOrGreater() {
        return getAPILevel() >= 19;
    }

    public static boolean isJellyBeanWithAirplaneRootHack() {
        return getAPILevel() == 16 || getAPILevel() == 17;
    }

    public static boolean isJellyBean43OrGreater() {
        return getAPILevel() > 17;
    }

    public static boolean isLollipopOrGreater() {
        return getAPILevel() >= 21;
    }

    public static boolean isMOrGreater() {
        return getAPILevel() >= 23;
    }

    public static boolean isNOrGreater() {
        return getAPILevel() >= 24;
    }

    public static boolean isJellyBean43() {
        return getAPILevel() == 18;
    }

    public static boolean isHoneycomb() {
        return getAPILevel() > 10 && getAPILevel() < 14;
    }

    public static boolean isX86() {
        return getCpuAbi().contains("x86");
    }

    public static String getCpuAbi() {
        if (cpuAbi == null) {
            try {
                cpuAbi = Build.CPU_ABI.toLowerCase();
            } catch (Throwable t) {
                cpuAbi = "Unknown";
            }
        }

        if (cpuAbi == null) {
            cpuAbi = "NULL";
        }

        return cpuAbi;
    }

    public static int getAPILevel() {
        if (apiLevel == null) {
            int apiLevelValue;
            try {
                // This field has been added in Android 1.6
                Field sdkInt = Build.VERSION.class.getField("SDK_INT");
                apiLevelValue = sdkInt.getInt(null);
            } catch (SecurityException e) {
                apiLevelValue = Integer.parseInt(Build.VERSION.SDK);
            } catch (NoSuchFieldException e) {
                apiLevelValue = Integer.parseInt(Build.VERSION.SDK);
            } catch (IllegalArgumentException e) {
                apiLevelValue = Integer.parseInt(Build.VERSION.SDK);
            } catch (IllegalAccessException e) {
                apiLevelValue = Integer.parseInt(Build.VERSION.SDK);
            }

            apiLevel = apiLevelValue;
        }


//        Logger.logInfo("API level " + apiLevel);
        return apiLevel;
    }

    public static String getManufacturer() {
        try {
            Field theMfrField = Build.class.getField("MANUFACTURER");
            return (String)theMfrField.get(null);
        } catch (SecurityException e) {
        } catch (NoSuchFieldException e) {
        } catch (IllegalArgumentException e) {
        } catch (IllegalAccessException e) {
        }

        return "UNK";
    }

    public static boolean isCyanogenMod(Context context) {
        PackageManager pm = context.getPackageManager();
        String version = System.getProperty("os.version");
        BufferedReader reader = null;

        try {
            if (version.contains("cyanogenmod") || pm.hasSystemFeature("com.cyanogenmod.android")) {
                return true;
            }

            // This does not require root
            reader = new BufferedReader(new FileReader("/proc/version"), 256);
            version = reader.readLine();

            if (version.contains("cyanogenmod")) {
                return true;
            }
        } catch (Exception e) {
            Logger.logWarning("Failed to check for cyanogem mod.", e);
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                }
            }
        }

        return false;
    }

    // Returns true/false if screen on detection is supported.
    // Returns null if it is not supported
    public static Boolean isScreenOn(Context context) {
        PowerManager pm = ((PowerManager) context.getSystemService(Context.POWER_SERVICE));
        try {
            Method isScreenOn = pm.getClass().getMethod("isScreenOn");
            return (Boolean) isScreenOn.invoke(pm);
        } catch (Exception e) {
            Logger.logInfo("Cannot fetch screen state", e);
            return null;
        }
    }

    // Returns true, if the application is signed with a debug certificate.
    public static boolean isDebugCertificateDoNotUse(Context ctx) {
        if (isDebugCertificate != null) {
            return isDebugCertificate;
        }

        try {
            PackageInfo pinfo = ctx.getPackageManager().getPackageInfo(ctx.getPackageName(), PackageManager.GET_SIGNATURES);
            Signature signatures[] = pinfo.signatures;

            for (int i = 0; i < signatures.length; i++) {
                CertificateFactory cf = CertificateFactory.getInstance("X.509");
                ByteArrayInputStream stream = new ByteArrayInputStream(signatures[i].toByteArray());
                X509Certificate cert = (X509Certificate) cf.generateCertificate(stream);
                isDebugCertificate = cert.getSubjectX500Principal().equals(DEBUG_DN);
                if (isDebugCertificate) {
                    return isDebugCertificate;
                }
            }

        } catch (Exception e) {
            Logger.logInfo("Failed to check debug certificate.", e);
            isDebugCertificate = false;
        }
        return isDebugCertificate;
    }

    private static Map<String, Boolean> writeableCache = new HashMap<String, Boolean>();

    public synchronized static boolean isWritable(File path) {
        // Cache writeable status for phones, that popup notification when folder is created. This will reduce spammyness on these phones.
        if (writeableCache.containsKey(path.getAbsolutePath())) {
            return writeableCache.get(path.getAbsolutePath());
        }

        boolean writeable = path.exists() && path.canRead() && path.canWrite() && canCreateFile(path);
        writeableCache.put(path.getAbsolutePath(), writeable);

        return writeable;
    }

    private static boolean canCreateFile(File path) {
        try {
            File newFile = new File(path, "directory-writable-test");
            try {
                newFile.createNewFile();
            } catch (IOException e) {
                Logger.logInfo("Cannot create file at: " + path);
                return false;
            }
            return newFile.delete();
        } catch (Exception e) {
            return false;
        }
    }

    public static File getExternalPublicWriteableStorage() {
        File globalExternalStorage = android.os.Environment.getExternalStorageDirectory();

        if (isKitKatOrGreater() && !isWritable(globalExternalStorage)) {
            return android.os.Environment.getExternalStoragePublicDirectory(getDocumentsFolder());
        }

        return globalExternalStorage;
    }

    public static File getLargestExternalPublicWriteableStorage(Context context) {
        File globalExternalStorage = android.os.Environment.getExternalStorageDirectory();

        if (isKitKatOrGreater()) {
            File[] files = getExternalDocumentsDirs(context);
            // TODO: find largest device
            return files.length > 0 ? files[0] : null;
        }

        return globalExternalStorage;
    }

    public static String getDocumentsFolder() {
        String result = "Documents";
        try {
            // Since KitKat
            Field docDir = android.os.Environment.class.getField("DIRECTORY_DOCUMENTS");
            result = (String) docDir.get(null);
        } catch (SecurityException e) {
        } catch (NoSuchFieldException e) {
        } catch (IllegalArgumentException e) {
        } catch (IllegalAccessException e) {
        }

        return result;
    }

    public static File[] getExternalDocumentsDirs(Context context) {
        try {
            return (File[]) context.getClass().getMethod("getExternalFilesDirs", String.class).invoke(context, getDocumentsFolder());
        } catch (SecurityException e) {
        } catch (NoSuchMethodException e) {
        } catch (InvocationTargetException e) {
        } catch (IllegalArgumentException e) {
        } catch (IllegalAccessException e) {
        }

        return new File[0];
    }

    private static String getCountryCodeFromSim(Context context) {
        String isoCode = ((TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE)).getSimCountryIso();
        if (isoCode != null) {
            isoCode = isoCode.toUpperCase();
        }
        return isoCode;
    }

    public static String getCountryCode(Context context) {
        // Keep old defaul behaviour to take country from timezone.
        String countryCode = TimeZoneCountryMap.getCountry(TimeZone.getDefault().getID());
        if (countryCode == null) {
            countryCode = getCountryCodeFromSim(context);
        } else if (countryCode.equals("DE") || countryCode.equals("NL")) {
            // We know DE and NL gets mixed in timezone based country. So for these 2 countries we make exception and we try to use SIM data if possible.
            String candidateCode = getCountryCodeFromSim(context);
            if (candidateCode != null && (candidateCode.equals("DE") || candidateCode.equals("NL"))) {
                // But the result must be one of the 2 candidate countries.
                countryCode = candidateCode;
            }
        }

        if (countryCode == null) {
            countryCode = Locale.getDefault().getCountry();
        }

        return countryCode;
    }

    public static boolean isUsingSwiftKey(Context context) {
        try {
            String currentKeyboard = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.DEFAULT_INPUT_METHOD);
            return currentKeyboard != null && currentKeyboard.contains("swiftkey");
        } catch (Exception e) {
            Logger.logWarning("Failed to check keyboard.", e);
            return false;
        }
    }

    public static String getCurrentProcessName(Context context){
        int myPid = android.os.Process.myPid();
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        if (manager == null) {
            return "";
        }

        for(ActivityManager.RunningAppProcessInfo processInfo : manager.getRunningAppProcesses()){
            if(processInfo.pid == myPid){
                return processInfo.processName;
            }
        }
        return "";
    }
}
