package com.samsung.microbit.data.constants;

import com.samsung.microbit.utils.MemoryUnits;

import java.util.concurrent.TimeUnit;

/**
 * Contains common constants.
 */
public class Constants {
    public static final int MAX_VIDEO_RECORDING_TIME_MILLIS = (int) TimeUnit.SECONDS.toMillis(30);
    public static final int VIDEO_FLASH_PICK_INTERVAL = (int) TimeUnit.SECONDS.toMillis(1);

    public static final int MAX_VIDEO_FILE_SIZE_BYTES = (int) MemoryUnits.Megabytes.instance().toBytes(100);

    public static final int PIC_COUNTER_DURATION_MILLIS = (int) TimeUnit.SECONDS.toMillis(5);
    public static final int PIC_COUNTER_INTERVAL_MILLIS = 900;

    public static final int MAX_COUNT_OF_RE_CONNECTIONS_FOR_DFU = 3;
    public static final long TIME_FOR_CONNECTION_COMPLETED = TimeUnit.SECONDS.toMillis(25);
    public static final long DELAY_BETWEEN_PAUSE_AND_RESUME = TimeUnit.SECONDS.toMillis(1);

    public static final String PREFERENCES = "Preferences";
    public static final String PREFERENCES_LIST_ORDER = "Preferences.listOrder";

    /**
     * Represents common states of connection.
     * It uses for sending appropriate statistics.
     */
    public enum ConnectionState {
        SUCCESS,
        FAIL,
        DISCONNECT
    }

    public static final long JUST_PAIRED_DELAY_ON_CONNECTION = 11000;

    public static final String MICROBIT_HEX_MIME_TYPE = "application/x-microbit-hex";
}
