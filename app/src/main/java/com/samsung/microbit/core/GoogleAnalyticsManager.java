package com.samsung.microbit.core;

import android.app.Activity;
import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.samsung.microbit.MBApp;
import com.samsung.microbit.R;
import com.samsung.microbit.core.bluetooth.BluetoothUtils;
import com.samsung.microbit.data.constants.Constants;
import com.samsung.microbit.utils.ProjectsHelper;

import java.util.HashMap;


/**
 * Allows to send statistics about app work, such as navigation, pairing,
 * connection and flashing statistics.
 */
public class GoogleAnalyticsManager {

    private static final String TAG = GoogleAnalyticsManager.class.getSimpleName();
    private static final String PREFS_NAME = "com.samsung.microbit";
    private static final String TRACK_ID = "UA-87395020-1";

    private static GoogleAnalyticsManager instance = null;

    private Tracker mTracker;

    private boolean mShareStatistic = false;

    private final static int MICROBITS_PAIRED = 1;
    private final static int SAVED_PROJECTS = 2;
    private final static int HEX_FILE_SIZE = 3;
    private final static int BINARY_SIZE = 4;
    private final static int FIRMWARE = 5;
    private final static int BUTTON = 6;
    private final static int DURATION = 7;
    private final static int HEX_FILE_FLASH_STATUS = 8;
    private final static int STATS_TRACKING_STATUS = 9;
    private final static int PAIR_STATUS = 10;
    private final static int CONNECT_STATUS = 11;


    private GoogleAnalyticsManager(Context context) {

        mShareStatistic = context.getSharedPreferences(PREFS_NAME, Context
                .MODE_PRIVATE).getBoolean(context.getString(R.string.prefs_share_stats_status), true);

        mTracker = GoogleAnalytics.getInstance(context).newTracker(TRACK_ID);
    }

    public static synchronized GoogleAnalyticsManager createInstance(MBApp mbApp) {
        if (instance == null) {
            instance = new GoogleAnalyticsManager(mbApp);
        }
        return instance;
    }

    /*
    Use this method to send events
     */
    public static GoogleAnalyticsManager getInstance() {
        return instance;
    }

    public void setShareStatistic(boolean shareStatistic) {
        this.mShareStatistic = shareStatistic;
    }

    public void activityStart(Activity activity) {
        if (mShareStatistic) {
            GoogleAnalytics.getInstance(activity).reportActivityStart(activity);
        }
    }

    public void activityStop(Activity activity) {
        if(mShareStatistic) {
            GoogleAnalytics.getInstance(activity).reportActivityStop(activity);
        }
    }

    private void sendEvent(final String screenName, final String category, final String action, final HashMap<Integer, String> dimensionsMap) {
        mTracker.setScreenName(screenName);

        HitBuilders.EventBuilder eventBuilder = new HitBuilders.EventBuilder()
                .setCategory(category)
                .setAction(action);

        for (int key : dimensionsMap.keySet()) {
            eventBuilder.setCustomDimension(key, dimensionsMap.get(key));
        }

        mTracker.send(eventBuilder.build());
    }

    public void sendAppStats(final String screenName) {
        if (mShareStatistic) {
            Log.d(TAG, "sendAppStats");
            HashMap<Integer, String> eventLabels = new HashMap<>();
            eventLabels.put(MICROBITS_PAIRED, Integer.toString(BluetoothUtils.getTotalPairedMicroBitsFromSystem()));
            eventLabels.put(SAVED_PROJECTS, Integer.toString(ProjectsHelper.getTotalSavedProjects()));

            sendEvent(screenName, "App", "App start", eventLabels);

        } else {
            Log.d(TAG, "Sharing of stats is disabled by user");
        }
    }


    public void sendViewEventStats(final String screenName) {
        if (mShareStatistic) {
            mTracker.setScreenName(screenName);
            mTracker.send(new HitBuilders.ScreenViewBuilder().build());
        } else {
            Log.d(TAG, "Sharing of stats is disabled by user");
        }
    }

    public void sendFlashStats(final String screenName, final boolean success, final  String fileName, final String hexsize, final String binsize, final String firmware) {
        try {
            if (mShareStatistic) {
                Log.d(TAG, "sendFlashStats fileName=" + fileName + " hexsize=" + hexsize + "  " +
                        "binsize=" + binsize + " microbit_firmwwareversion= " + firmware);
                HashMap<Integer, String> eventLabels = new HashMap<>();
                eventLabels.put(HEX_FILE_SIZE, hexsize);
                eventLabels.put(BINARY_SIZE, binsize);
                eventLabels.put(FIRMWARE, firmware);

                if (success) {
                    eventLabels.put(HEX_FILE_FLASH_STATUS, "success");
                } else {
                    eventLabels.put(HEX_FILE_FLASH_STATUS, "fail");
                }

                sendEvent(screenName, "Flash", "Hex file flash", eventLabels);

            } else {
                Log.d(TAG, "Sharing of stats is disabled by user");
            }
        } catch (RuntimeException e) {
            Log.e(TAG, "Sending stats exception " + e.getMessage());
        }
    }

    public void sendNavigationStats(final String screenName, final String button) {
        try {
            if (mShareStatistic) {
                HashMap<Integer, String> eventLabels = new HashMap<>();
                eventLabels.put(BUTTON, button);

                sendEvent(screenName, "Navigation", "Click", eventLabels);

            } else {
                Log.d(TAG, "Sharing of stats is disabled by user");
            }
        } catch (RuntimeException e) {
            Log.e(TAG, "Sending stats exception " + e.getMessage());
        }
    }

    public void sendStatSharing(final String screenName, final boolean enable) {
        try {
            HashMap<Integer, String> eventLabels = new HashMap<>();
            if (enable) {
                eventLabels.put(STATS_TRACKING_STATUS, "opt-in");
            } else {
                eventLabels.put(STATS_TRACKING_STATUS, "opt-out");
            }

            sendEvent(screenName, "Share Statistics", "Change stats option", eventLabels);

        } catch (RuntimeException e) {
            Log.e(TAG, "Sending stats exception " + e.getMessage());
        }
    }

    public void sendPairingStats(final String screenName, final boolean paired, final String firmware) {
        try {
            if (mShareStatistic) {
                HashMap<Integer, String> eventLabels = new HashMap<>();
                eventLabels.put(FIRMWARE, TextUtils.isEmpty(firmware) ? "-" : firmware);

                if (paired) {
                    eventLabels.put(PAIR_STATUS, "success");
                } else {
                    eventLabels.put(PAIR_STATUS, "fail");
                }

                sendEvent(screenName, "Pairing", "Pair", eventLabels);
            } else {
                Log.d(TAG, "Sharing of stats is disabled by user");
            }
        } catch (RuntimeException e) {
            Log.e(TAG, "Sending stats exception " + e.getMessage());
        }
    }

    public void sendConnectStats(final String screenName, final Constants.ConnectionState connectionState, final String firmware, final String duration) {
        try {
            if (mShareStatistic) {
                HashMap<Integer, String> eventLabels = new HashMap<>();
                Log.d(TAG, "sendConnectStats, firmware " + firmware + " duration " + duration + " connectionState " + connectionState);

                eventLabels.put(FIRMWARE, TextUtils.isEmpty(firmware) ? "-" : firmware);
                eventLabels.put(DURATION, TextUtils.isEmpty(duration) ? "-" : duration );
                switch (connectionState) {
                    case SUCCESS:
                        Log.d(TAG, "Sending Connection stats - MSG(SUCCESS) - Firmware = " + firmware);
                        eventLabels.put(CONNECT_STATUS, "success");
                        break;
                    case FAIL:
                        Log.d(TAG, "Sending Connection stats - MSG(Failed)");
                        eventLabels.put(CONNECT_STATUS, "fail");

                        break;
                    case DISCONNECT:
                        Log.d(TAG, "Sending Connection stats - MSG(DISCONNECT) - Firmware = " + firmware + " Duration =" + duration);
                        eventLabels.put(CONNECT_STATUS, "disconnect");
                        break;
                    default:
                        break;
                }

                sendEvent(screenName, "Connection", "Connect", eventLabels);
            } else {
                Log.d(TAG, "Sharing of stats is disabled by user");
            }
        } catch (RuntimeException e) {
            Log.e(TAG, "Sending stats exception " + e.getMessage());
        }
    }
}
