package com.samsung.microbit.data.constants;

/**
 * Events sub codes, that Samsung devices respond to.
 * It commonly uses in plugins to identify appropriate event.
 */
public class EventSubCodes {
    private EventSubCodes() {
    }

    /**
     * Samsung remote control sub codes. Related to event Category {@link EventCategories#SAMSUNG_REMOTE_CONTROL_ID}
     *
     * @see com.samsung.microbit.utils.Utils#makeMicroBitValue(int, int) Utils.makeBicrobitValue(int, int)
     */
    public static final int SAMSUNG_REMOTE_CONTROL_EVT_PLAY = 1;
    public static final int SAMSUNG_REMOTE_CONTROL_EVT_PAUSE = 2;
    public static final int SAMSUNG_REMOTE_CONTROL_EVT_STOP = 3;
    public static final int SAMSUNG_REMOTE_CONTROL_EVT_NEXTTRACK = 4;
    public static final int SAMSUNG_REMOTE_CONTROL_EVT_PREVTRACK = 5;
    public static final int SAMSUNG_REMOTE_CONTROL_EVT_FORWARD = 6;
    public static final int SAMSUNG_REMOTE_CONTROL_EVT_REWIND = 7;
    public static final int SAMSUNG_REMOTE_CONTROL_EVT_VOLUMEUP = 8;
    public static final int SAMSUNG_REMOTE_CONTROL_EVT_VOLUMEDOWN = 9;

    /**
     * Samsung remote control sub codes. Related to event Category {@link EventCategories#SAMSUNG_CAMERA_ID}
     *
     * @see com.samsung.microbit.utils.Utils#makeMicroBitValue(int, int) Utils.makeBicrobitValue(int, int)
     */
    public static final int SAMSUNG_CAMERA_EVT_LAUNCH_PHOTO_MODE = 1;
    public static final int SAMSUNG_CAMERA_EVT_LAUNCH_VIDEO_MODE = 2;
    public static final int SAMSUNG_CAMERA_EVT_TAKE_PHOTO = 3;
    public static final int SAMSUNG_CAMERA_EVT_START_VIDEO_CAPTURE = 4;
    public static final int SAMSUNG_CAMERA_EVT_STOP_VIDEO_CAPTURE = 5;
    public static final int SAMSUNG_CAMERA_EVT_STOP_PHOTO_MODE = 6;
    public static final int SAMSUNG_CAMERA_EVT_STOP_VIDEO_MODE = 7;
    public static final int SAMSUNG_CAMERA_EVT_TOGGLE_FRONT_REAR = 8;

    /**
     * Samsung remote control sub codes. Related to event Category {@link EventCategories#SAMSUNG_AUDIO_RECORDER_ID}
     *
     * @see com.samsung.microbit.utils.Utils#makeMicroBitValue(int, int) Utils.makeBicrobitValue(int, int)
     */
    public static final int SAMSUNG_AUDIO_RECORDER_ID = 1003; //0x03EB
    public static final int SAMSUNG_AUDIO_RECORDER_EVT_LAUNCH = 0;
    public static final int SAMSUNG_AUDIO_RECORDER_EVT_START_CAPTURE = 1;
    public static final int SAMSUNG_AUDIO_RECORDER_EVT_STOP_CAPTURE = 2;
    public static final int SAMSUNG_AUDIO_RECORDER_EVT_STOP = 3;

    /**
     * Samsung remote control sub codes. Related to event Category {@link EventCategories#SAMSUNG_ALERTS_ID}
     *
     * @see com.samsung.microbit.utils.Utils#makeMicroBitValue(int, int) Utils.makeBicrobitValue(int, int)
     */
    public static final int SAMSUNG_ALERT_EVT_DISPLAY_TOAST = 1;
    public static final int SAMSUNG_ALERT_EVT_VIBRATE = 2;
    public static final int SAMSUNG_ALERT_EVT_PLAY_SOUND = 3;
    public static final int SAMSUNG_ALERT_EVT_PLAY_RINGTONE = 4;
    public static final int SAMSUNG_ALERT_EVT_FIND_MY_PHONE = 5;
    public static final int SAMSUNG_ALERT_EVT_ALARM1 = 6;
    public static final int SAMSUNG_ALERT_EVT_ALARM2 = 7;
    public static final int SAMSUNG_ALERT_EVT_ALARM3 = 8;
    public static final int SAMSUNG_ALERT_EVT_ALARM4 = 9;
    public static final int SAMSUNG_ALERT_EVT_ALARM5 = 10;
    public static final int SAMSUNG_ALERT_EVT_ALARM6 = 11;
    public static final int SAMSUNG_ALERT_STOP_PLAYING = 12;


    /**
     * Events that Samsung devices generate:
     */

    /**
     * Samsung remote control sub codes. Related to event Category {@link EventCategories#SAMSUNG_SIGNAL_STRENGTH_ID}
     *
     * @see com.samsung.microbit.utils.Utils#makeMicroBitValue(int, int) Utils.makeBicrobitValue(int, int)
     */
    public static final int SAMSUNG_SIGNAL_STRENGTH_EVT_NO_BAR = 1;
    public static final int SAMSUNG_SIGNAL_STRENGTH_EVT_ONE_BAR = 2;
    public static final int SAMSUNG_SIGNAL_STRENGTH_EVT_TWO_BAR = 3;
    public static final int SAMSUNG_SIGNAL_STRENGTH_EVT_THREE_BAR = 4;
    public static final int SAMSUNG_SIGNAL_STRENGTH_EVT_FOUR_BAR = 5;

    /**
     * Samsung remote control sub codes. Related to event Category {@link EventCategories#SAMSUNG_DEVICE_INFO_ID}
     *
     * @see com.samsung.microbit.utils.Utils#makeMicroBitValue(int, int) Utils.makeBicrobitValue(int, int)
     */
    public static final int SAMSUNG_DEVICE_ORIENTATION_LANDSCAPE = 1;
    public static final int SAMSUNG_DEVICE_ORIENTATION_PORTRAIT = 2;
    public static final int SAMSUNG_DEVICE_GESTURE_NONE = 3;
    public static final int SAMSUNG_DEVICE_GESTURE_DEVICE_SHAKEN = 4;
    public static final int SAMSUNG_DEVICE_DISPLAY_OFF = 5;
    public static final int SAMSUNG_DEVICE_DISPLAY_ON = 6;
    public static final int SAMSUNG_INCOMING_CALL = 7;
    public static final int SAMSUNG_INCOMING_SMS = 8;
    public static final int SAMSUNG_DEVICE_BATTERY_STRENGTH = 9; // not specified
}
