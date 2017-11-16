package com.urbandroid.sleep.garmin.logging;

/**
 * Created by artaud on 16.11.17.
 */

import android.content.Context;
import android.util.Log;

import com.urbandroid.sleep.garmin.logging.EmulatorDetector;
import com.urbandroid.sleep.garmin.logging.Environment;
import com.urbandroid.sleep.garmin.logging.ApplicationVersionExtractor;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Logger {
    public static String defaultTag = "UNSPECIFIED";
    public static final boolean STORE_LOG = false;

    private static LogConfig DEFAULT_LOG = null;

    private static Map<LogConfig, LinkedList<LogRecord>> logBuffers;
    private static Set<LogConfig> logConfigs = new HashSet<LogConfig>();
    //private static int DEFAULT_MAX_BUFFER_LENGTH = 2000;

    public static final int VERBOSE_LEVEL = 0;
    public static final int DEBUG_LEVEL = 1;
    public static final int INFO_LEVEL = 2;
    public static final int WARNING_LEVEL = 3;
    public static final int SEVERE_LEVEL = 4;

    private static final String[] LEVEL_NAMES = new String[] { "VERBOSE", "DEBUG", "INFO", "WARN", "SEVERE" };

    private static final int releaseLogLevel = INFO_LEVEL;
    private static final int emulatorLoglevel = DEBUG_LEVEL;

    private static int loglevel = releaseLogLevel;
    private static int logInMemorylevel = releaseLogLevel;
    private static int appVersion;

    private static Map<LogConfig, BufferedWriter> currentLogWriters = new HashMap<LogConfig, BufferedWriter>();
    private static Map<LogConfig, Integer> currentLogWriterLinesWritten = new HashMap<LogConfig, Integer>();
    private static Map<LogConfig, Long> currentLogWriterBytesWritten = new HashMap<LogConfig, Long>();
    private static Map<LogConfig, LinkedList<LogRecord>> logWriteBuffers;

    private static LogFlusher logFlusher;

    private static ThreadLocal<DateFormat> dateFormat = new ThreadLocal<DateFormat>() {
        protected DateFormat initialValue() {
            return new SimpleDateFormat("HH:mm:ss.SSS");
        };
    };

    private static ThreadLocal<DateFormat> dateFormatWithDate = new ThreadLocal<DateFormat>() {
        protected DateFormat initialValue() {
            return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
        };
    };

    public static void initialize(Context context, String defaultTag, int maxInMemoryBufferLengthNOT_USED, int logLevel, int inMemoryLogLevel) {
        initialize(context, defaultTag, logLevel, inMemoryLogLevel);
    }

    public static void initialize(Context context, String defaultTag, int logLevel, int inMemoryLogLevel, LogConfig... configs) {
        String processName = Environment.getCurrentProcessName(context);
        boolean initForMainProcess = true;
        String logName = "logbuffer";
        if (processName != null && !context.getPackageName().equals(processName)) {
            logName = logName + "_" + processName;
            initForMainProcess = false;
        }
        DEFAULT_LOG = new LogConfig("DEFAULT", logName, true, 100000, 5, 15000, 2000);

        appVersion = new ApplicationVersionExtractor().getCurrentVersion(context).getVersionCode();
        Logger.loglevel = logLevel;
        Logger.logInMemorylevel = inMemoryLogLevel;
        logConfigs.add(DEFAULT_LOG);
        if (configs != null) {
            for (LogConfig config : configs) {
                logConfigs.add(config);
            }
        }
        initLogBuffer();
        Logger.defaultTag = defaultTag;

        if (context != null && context.getCacheDir() != null && context.getCacheDir().getAbsoluteFile() != null) {
            // Non-main process is flushed often as we cannot read it from memory.
            int flushFrequencySeconds = initForMainProcess ? 60 : 1;
            // We can use in-file logging in cache-dir
            logFlusher = new LogFlusher(context.getApplicationContext(), flushFrequencySeconds);
            new Thread(logFlusher, "Log-Flusher").start();
        }
    }

    private static void initLogBuffer() {
        logBuffers = new HashMap<LogConfig, LinkedList<LogRecord>>();
        for (LogConfig config : logConfigs) {
            logBuffers.put(config, new LinkedList<LogRecord>());
        }
    }

    public static boolean isInitialized() {
        return !defaultTag.equals("UNSPECIFIED");
    }

    private static String getCurrentLogFileName(Context context, LogConfig config) {
        return context.getCacheDir().getAbsolutePath() + File.separator + config.logFileName;
    }

    private static String getOldLogFileName(Context context, LogConfig config, int version) {
        if (version == 1) {
            return getCurrentLogFileName(context, config);
        }
        return getCurrentLogFileName(context, config) + "." + version;
    }

    public static void initialize(Context context, String defaultTag, int maxInMemoryBufferLengthNOT_USED) {
        int loglevel = EmulatorDetector.isEmulator(context) ? emulatorLoglevel : releaseLogLevel;
        int logInMemorylevel = loglevel;

        initialize(context, defaultTag, maxInMemoryBufferLengthNOT_USED, loglevel, logInMemorylevel);
    }

    public static DateFormat getDateFormat() {
        return dateFormat.get();
    }

    private static String formatMessage(LogConfig config, long timestamp, String message) {
        return formatMessage(config, timestamp, message, Thread.currentThread().getName());
    }

    private static String formatMessage(LogConfig config, long timestamp, String message, String threadName) {
        boolean writeHeader = true;
        if (config != null) {
            writeHeader = config.writeHeader;
        }

        DateFormat format = writeHeader ? dateFormat.get() : dateFormatWithDate.get();
        String version = writeHeader ? "" : ", " + appVersion;
        return "[" + format.format(new Date(timestamp)) + ", " + threadName + version + "]: " + message;
    }

    private static void appendToBuffer(int level, long timestamp, String tag, String message, Throwable throwable) {
        appendToBuffer(DEFAULT_LOG, level, timestamp, tag, message, throwable);
    }

    private static void appendToBuffer(LogConfig config, int level, long timestamp, String tag, String message, Throwable throwable) {
        if (config.maxInMemoryLines > 0) {
            LogRecord record = new LogRecord(config, level, timestamp, tag, message, Thread.currentThread().getName(), throwable);
            synchronized (Logger.class) {
                if (logBuffers != null) {
                    LinkedList<LogRecord> buffer = logBuffers.get(config);
                    if (buffer != null) {
                        buffer.addLast(record);
                        if (buffer.size() > config.maxInMemoryLines)
                            buffer.removeFirst();
                    }
                }
            }
        }
    }

    public static void persistBuffer() {
        if (logFlusher != null) {
            logFlusher.forceFlush(false);
        }
    }

    public static void syncPersistBuffer() {
        if (logFlusher != null) {
            logFlusher.forceFlush(true);
        }
    }


    private static void logInMemory(LogConfig config, int level, long timestamp, String tag, String message, Throwable throwable) {
        if (!isInitialized()) {
            Log.e(defaultTag, "Calling log before initializing logger.", new Exception("MARKER"));
            return;
        }

        if (logInMemorylevel > level) {
            return;
        }

        appendToBuffer(config,  level, timestamp, tag, message, throwable);
    }

    public static void logVerbose(String tag, String message, Throwable throwable) {
        long timestamp = System.currentTimeMillis();
        logInMemory(DEFAULT_LOG, VERBOSE_LEVEL, timestamp, tag, message, throwable);

        if ( loglevel > VERBOSE_LEVEL )
            return;

        if ( throwable != null ) {
            Log.v(tag, formatMessage(DEFAULT_LOG, timestamp, message), throwable);
        } else {
            Log.v(tag, formatMessage(DEFAULT_LOG, timestamp, message));
        }
    }

    public static void logVerbose(String message, Throwable throwable) {
        logVerbose(defaultTag, message, throwable);
    }

    public static void logVerbose(String message) {
        logVerbose(defaultTag, message, null);
    }


    public static void logDebug(String tag, String message, Throwable throwable) {
        long timestamp = System.currentTimeMillis();
        logInMemory(DEFAULT_LOG, DEBUG_LEVEL, timestamp, tag, message, throwable);

        if ( loglevel > DEBUG_LEVEL )
            return;

        if ( throwable != null ) {
            Log.d(tag, formatMessage(DEFAULT_LOG, timestamp, message), throwable);
        } else {
            Log.d(tag, formatMessage(DEFAULT_LOG, timestamp, message));
        }
    }

    public static void logDebug(String message, Throwable throwable) {
        logDebug(defaultTag, message, throwable);
    }

    public static void logDebug(String message) {
        logDebug(defaultTag, message, null);
    }


    public static void logInfo(String tag, String message, Throwable throwable) {
        logInfo(DEFAULT_LOG, tag, message, throwable);
    }

    public static void logInfo(LogConfig config, String tag, String message, Throwable throwable) {
        long timestamp = System.currentTimeMillis();
        logInMemory(config, INFO_LEVEL, timestamp, tag, message, throwable);

        if ( loglevel > INFO_LEVEL )
            return;

        if ( throwable != null ) {
            Log.i(tag, formatMessage(config, timestamp, message), throwable);
        } else {
            Log.i(tag, formatMessage(config, timestamp, message));
        }
    }

    public static void logInfo(String message, Throwable throwable) {
        logInfo(defaultTag, message, throwable);
    }

    public static void logInfo(String message) {
        logInfo(defaultTag, message, null);
    }

    public static void logInfo(LogConfig config, String message) {
        logInfo(config, defaultTag, message, null);
    }

    public static void logWarning(String tag, String message, Throwable throwable) {
        long timestamp = System.currentTimeMillis();
        logInMemory(DEFAULT_LOG, WARNING_LEVEL, timestamp, tag, message, throwable);

        if ( loglevel > WARNING_LEVEL )
            return;

        if ( throwable != null ) {
            Log.w(tag, formatMessage(DEFAULT_LOG, timestamp, message), throwable);
        } else {
            Log.w(tag, formatMessage(DEFAULT_LOG, timestamp, message));
        }
    }

    public static void logWarning(String message, Throwable throwable) {
        logWarning(defaultTag, message, throwable);
    }

    public static void logWarning(String message) {
        logWarning(defaultTag, message, null);
    }

    public static void logSevere(String tag, String message, Throwable throwable) {
        long timestamp = System.currentTimeMillis();
        logInMemory(DEFAULT_LOG, SEVERE_LEVEL, timestamp, tag, message, throwable);

        if ( loglevel > SEVERE_LEVEL )
            return;

        if ( throwable != null ) {
            Log.e(tag, formatMessage(DEFAULT_LOG, timestamp, message), throwable);
        } else {
            Log.e(tag, formatMessage(DEFAULT_LOG, timestamp, message));
        }
    }

    public static void logSevere(String message, Throwable throwable) {
        logSevere(defaultTag, message, throwable);
    }

    public static void logSevere(Throwable throwable) {
        logSevere(defaultTag, "Exception occurred, no description given", throwable);
    }

    public static void logSevere(String message) {
        logSevere(defaultTag, message, null);
    }

    public static List<LogRecord> getLogBuffer() {
        synchronized (Logger.class) {
            return new LinkedList<LogRecord>(logBuffers.get(DEFAULT_LOG));
        }
    }

    public static List<LogRecord> getLogBufferIncluingWriteBuffer() {
        return getLogBufferIncluingWriteBuffer(DEFAULT_LOG);
    }

    public static List<LogRecord> getLogBufferIncluingWriteBuffer(LogConfig config) {
        synchronized (Logger.class) {
            LinkedList<LogRecord> result = new LinkedList<LogRecord>();
            if (logWriteBuffers != null && logWriteBuffers.get(config) != null) {
                result.addAll(logWriteBuffers.get(config));
            }
            if (logBuffers != null && logBuffers.get(config) != null) {
                result.addAll(logBuffers.get(config));
            }
            return result;
        }
    }

    public static File[] getPersistentBufferFiles(Context context) {
        return getPersistentBufferFiles(context, DEFAULT_LOG);
    }

    public static File[] getPersistentBufferFiles(Context context, LogConfig config) {
        List<File> files = new LinkedList<File>();
        File logFile = new File(getCurrentLogFileName(context, config));
        if (logFile.exists()) {
            // Sort files from oldest to newest, because the lines are also sorted this way.
            for (int i = config.maxLogFiles; i > 0; i--) {
                File oldLogFile = new File(getOldLogFileName(context, config, i));
                if (oldLogFile.exists()) {
                    //result += InputStreamUtil.read(new FileInputStream(oldLogFile));
                    files.add(oldLogFile);
                }
            }
            return files.toArray(new File[0]);
        }
        return files.toArray(new File[0]);
    }

    public static String appendStackTrace(StackTraceElement[] elements) {
        try {
            StringBuilder result = new StringBuilder();
            for (StackTraceElement element : elements) {
                result.append("\t").append(element.toString()).append("\n");
            }
            return result.toString();
        } catch ( Exception e ) {
            return "Failed to serialize report. Error: " + e.getClass() + " ->" + e.getMessage();
        }
    }

    private static final String separator = "|-o-|";

    public static class LogRecord {
        final LogConfig config;
        final int level;
        final long timestamp;
        final String tag;
        final String message;
        final String threadName;
        final Throwable throwable;

        public LogRecord(LogConfig config, int level, long timestamp, String tag, String message, String threadName, Throwable throwable) {
            this.config = config;
            this.level = level;
            this.timestamp = timestamp;
            this.tag = tag;
            this.message = message;
            this.threadName = threadName;
            this.throwable = throwable;
        }

        public int getLevel() {
            return level;
        }

        public String getLevelName() {
            return LEVEL_NAMES[level];
        }

        public long getTimestamp() {
            return timestamp;
        }

        public String getTag() {
            return tag;
        }

        public String getMessage() {
            return message;
        }

        public String getThreadName() {
            return threadName;
        }

        public Throwable getThrowable() {
            return throwable;
        }

        public String getFormattedRecord() {
            StringBuilder builder = new StringBuilder();
            builder.append(tag + "." + getLevelName() + ": " + formatMessage(config, timestamp, message, threadName) + (throwable == null ? "" : " " + throwable.getClass().getName() + ": " + throwable.getMessage()));
            if (throwable != null) {
                builder.append("\n" + appendStackTrace(throwable.getStackTrace()));
            }
            return builder.toString();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof LogRecord)) return false;

            LogRecord logRecord = (LogRecord) o;
            return level == logRecord.level && timestamp == logRecord.timestamp && !(message != null ? !message.equals(logRecord.message) : logRecord.message != null) && !(tag != null ? !tag.equals(logRecord.tag) : logRecord.tag != null) && !(threadName != null ? !threadName.equals(logRecord.threadName) : logRecord.threadName != null);
        }

        @Override
        public int hashCode() {
            int result = level;
            result = 31 * result + (int) (timestamp ^ (timestamp >>> 32));
            result = 31 * result + (tag != null ? tag.hashCode() : 0);
            result = 31 * result + (message != null ? message.hashCode() : 0);
            result = 31 * result + (threadName != null ? threadName.hashCode() : 0);
            return result;
        }
    }

    private static class LogFlusher implements Runnable {
        private final Context context;
        private final Object FLUSH_COND = new Object();
        private final Set<Object> waiters = new HashSet<Object>();
        private final int flushFrequencySeconds;

        private LogFlusher(Context context, int flushFrequencySeconds) {
            this.context = context;
            this.flushFrequencySeconds = flushFrequencySeconds;
        }

        @Override
        public void run() {
            try {
                while (!Thread.interrupted()) {
                    synchronized (FLUSH_COND) {
                        FLUSH_COND.wait(1000 * flushFrequencySeconds);
                    }

                    synchronized (Logger.class) {
                        logWriteBuffers = logBuffers;
                        initLogBuffer();
                    }

                    if (logWriteBuffers == null) {
                        continue;
                    }

                    for (Map.Entry<LogConfig, LinkedList<LogRecord>> entry : logWriteBuffers.entrySet()) {
                        LogConfig config = entry.getKey();
                        LinkedList<LogRecord> buffer = entry.getValue();
                        BufferedWriter currentLogWriter = currentLogWriters.get(config);
                        if (buffer.isEmpty()) {
                            continue;
                        }

                        try {
                            if (currentLogWriter == null) {
                                long initialLength = rotateLogFilesIfRequired(context, config);
                                currentLogWriterBytesWritten.put(config, initialLength);
                                currentLogWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(getCurrentLogFileName(context, config), true)));
                                currentLogWriters.put(config, currentLogWriter);
                                if (config.writeHeader) {
                                    currentLogWriter.write("************ Log file started [" + new Date() + "] Version: " + new ApplicationVersionExtractor().getCurrentVersion(context) + " **************");
                                }
                                currentLogWriter.newLine();
                            }

                            int linesWritten = 0;
                            long bytesWritten = 0;
                            if (currentLogWriterLinesWritten.get(config) != null) {
                                linesWritten = currentLogWriterLinesWritten.get(config);
                            }
                            if (currentLogWriterBytesWritten.get(config) != null) {
                                bytesWritten = currentLogWriterBytesWritten.get(config);
                            }
                            for (LogRecord record : buffer) {
                                String formatedRecord = record.getFormattedRecord();
                                currentLogWriter.write(formatedRecord);
                                currentLogWriter.newLine();
                                linesWritten++;
                                // Note, this is just approx. Real written length is likely a bit higher if we have
                                // a lot of non ASCII chars. But should not be much higher..
                                bytesWritten += formatedRecord.length() + 1;
                            }

                            currentLogWriterLinesWritten.put(config, linesWritten);
                            currentLogWriterBytesWritten.put(config, bytesWritten);

                            currentLogWriter.flush();

                            synchronized (Logger.class) {
                                buffer.clear();
                            }

                            synchronized (FLUSH_COND) {
                                for (Object waiter : waiters) {
                                    synchronized (waiter) {
                                        waiter.notifyAll();
                                    }
                                }
                            }

                            linesWritten = 0;
                            bytesWritten = 0;
                            if (currentLogWriterLinesWritten.get(config) != null) {
                                linesWritten = currentLogWriterLinesWritten.get(config);
                            }
                            if (currentLogWriterBytesWritten.get(config) != null) {
                                bytesWritten = currentLogWriterBytesWritten.get(config);
                            }
                            /*
                            if (linesWritten > config.maxLinesPerLogFile) {
                                currentLogWriterLinesWritten.put(config, 0);
                                currentLogWriter.close();
                                currentLogWriters.remove(config);
                            }*/
                            if (bytesWritten > config.minLengthToRotate) {
                                currentLogWriter.write("Written bytes: " + bytesWritten + " -> rotating");
                                currentLogWriterBytesWritten.put(config, 0l);
                                currentLogWriter.close();
                                currentLogWriters.remove(config);
                            }
                        } catch (IOException ignore) {
                            Logger.logSevere(ignore);
                        }
                    }
                }
            } catch (InterruptedException ie) {
                return;
            }
        }

        void forceFlush(boolean sync) {
            Object waiter = new Object();
            synchronized (FLUSH_COND) {
                FLUSH_COND.notifyAll();

                if (sync) {
                    waiters.add(waiter);
                }
            }

            try {
                if (sync) {
                    synchronized (waiter) {
                        waiter.wait(1000 * 5);
                    }
                }
            } catch (InterruptedException ie) {
                return;
            }
        }

        private long rotateLogFilesIfRequired(Context context, LogConfig config) {
            File currentLogFile = new File(getCurrentLogFileName(context, config));
            long currentLogFileLength = currentLogFile.exists() ? currentLogFile.length() : 0;
            if (!currentLogFile.exists() || currentLogFileLength < config.minLengthToRotate) {
                // No need to rotate yet.
                Logger.logDebug("No need to rotate log (" + config.logFileName + "), too small: " + currentLogFileLength);
                return currentLogFileLength;
            }
            Logger.logDebug("Rotating log files: " + config.logFileName);

            File lastFile = new File(getOldLogFileName(context, config, config.maxLogFiles));
            if (lastFile.exists()) {
                lastFile.delete();
            }
            for (int i = config.maxLogFiles - 1; i > 0; i--) {
                File toBeRenamed = new File(getOldLogFileName(context, config, i));
                if (toBeRenamed.exists()) {
                    toBeRenamed.renameTo(new File(getOldLogFileName(context, config, i + 1)));
                }
            }

            return 0;
        }
    }

    public static class LogConfig {
        public final String logName;
        final String logFileName;
        final boolean writeHeader;
        final int minLengthToRotate;
        final int maxLogFiles;
        final int maxLinesPerLogFile;
        final int maxInMemoryLines;

        public LogConfig(String logName, String logFileName, boolean writeHeader, int minLengthToRotate, int maxLogFiles, int maxLinesPerLogFile, int maxInMemoryLines) {
            this.logName = logName;
            this.logFileName = logFileName;
            this.writeHeader = writeHeader;
            this.minLengthToRotate = minLengthToRotate;
            this.maxLogFiles = maxLogFiles;
            this.maxLinesPerLogFile = maxLinesPerLogFile;
            this.maxInMemoryLines = maxInMemoryLines;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            LogConfig logConfig = (LogConfig) o;

            return logFileName.equals(logConfig.logFileName);

        }

        @Override
        public int hashCode() {
            return logFileName.hashCode();
        }
    }
}
