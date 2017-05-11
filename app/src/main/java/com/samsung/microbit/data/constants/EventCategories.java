package com.samsung.microbit.data.constants;

/**
 * Event categories, that Samsung devices respond to.
 */
public class EventCategories {
    private EventCategories() {
    }

    public static final int CATEGORY_UNKNOWN = 32;

    public static final int IPC_BLE_DISCONNECT = 1;
    public static final int IPC_BLE_CONNECT = 2;
    public static final int IPC_BIND_SERVICES = 10;

    public static final int IPC_BLE_RECONNECT = 3;
    public static final int IPC_WRITE_CHARACTERISTIC = 4;
    public static final int IPC_DISCONNECT_FOR_FLASH = 5;
    public static final int IPC_PLUGIN_STOP_PLAYING = 6;

    public static final int CATEGORY_REPLY = 8;

    public static final int IPC_BLE_NOTIFICATION_GATT_CONNECTED = 4000;
    public static final int IPC_BLE_NOTIFICATION_GATT_DISCONNECTED = 4001;
    public static final int IPC_BLE_NOTIFICATION_CHARACTERISTIC_CHANGED = 4002;

    public static final int IPC_BLE_NOTIFICATION_INCOMING_CALL = 4003;
    public static final int IPC_BLE_NOTIFICATION_INCOMING_SMS = 4002;

    /**
     * Category for device info
     * <p/>
     * Check {@link EventSubCodes} for paired subcodes.
     *
     * @see EventSubCodes
     * @see com.samsung.microbit.utils.Utils#makeMicroBitValue(int, int) Utils.makeBicrobitValue(int, int)
     */
    public static final int SAMSUNG_DEVICE_INFO_ID = 1103; //0x044F

    /**
     * Category for internal registration
     * <p/>
     * <strong>
     * DON'T DELETE. Used for internal proposes!!!!
     * </strong>
     */
    public static final int SAMSUNG_TELEPHONY_ID = 5555;

    /**
     * Category for signal strength
     * <p/>
     * Check {@link EventSubCodes} for paired subcodes.
     *
     * @see EventSubCodes
     * @see com.samsung.microbit.utils.Utils#makeMicroBitValue(int, int) Utils.makeBicrobitValue(int, int)
     */
    public static final int SAMSUNG_SIGNAL_STRENGTH_ID = 1101; //0x044D

    /**
     * Category for remote controls
     * <p/>
     * Check {@link EventSubCodes} for paired subcodes.
     *
     * @see EventSubCodes
     * @see com.samsung.microbit.utils.Utils#makeMicroBitValue(int, int) Utils.makeBicrobitValue(int, int)
     */
    public static final int SAMSUNG_REMOTE_CONTROL_ID = 1001; //0x03E9

    /**
     * Category for alerts
     * <p/>
     * Check {@link EventSubCodes} for paired subcodes.
     *
     * @see EventSubCodes
     * @see com.samsung.microbit.utils.Utils#makeMicroBitValue(int, int) Utils.makeBicrobitValue(int, int)
     */
    public static final int SAMSUNG_ALERTS_ID = 1004; //0x03EC

    /**
     * Category for audio records.
     * <p/>
     * Check {@link EventSubCodes} for paired subcodes.
     *
     * @see EventSubCodes
     * @see com.samsung.microbit.utils.Utils#makeMicroBitValue(int, int) Utils.makeBicrobitValue(int, int)
     */
    public static final int SAMSUNG_AUDIO_RECORDER_ID = 1003; //0x03EB

    /**
     * Category for camera
     * <p/>
     * Check {@link EventSubCodes} for paired subcodes.
     *
     * @see EventSubCodes
     * @see com.samsung.microbit.utils.Utils#makeMicroBitValue(int, int) Utils.makeBicrobitValue(int, int)
     */
    public static final int SAMSUNG_CAMERA_ID = 1002;//0x03EA
}
