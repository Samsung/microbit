package com.samsung.microbit.plugin;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.samsung.microbit.MBApp;
import com.samsung.microbit.data.constants.EventSubCodes;
import com.samsung.microbit.data.model.CmdArg;
import com.samsung.microbit.ui.activity.AudioRecorderActivity;

/**
 * Provides actions for record audio sounds.
 */
public class AudioRecordPlugin implements AbstractPlugin {
    private static final String TAG = AudioRecordPlugin.class.getSimpleName();

    public static final String INTENT_ACTION_LAUNCH = "com.samsung.microbit.ui.activity.AudioRecorderActivity.action" +
            ".LAUNCH";
    public static final String INTENT_ACTION_START_RECORD = "com.samsung.microbit.ui.activity.AudioRecorderActivity" +
            ".action.START_RECORD";
    public static final String INTENT_ACTION_STOP_RECORD = "com.samsung.microbit.ui.activity.AudioRecorderActivity" +
            ".action.STOP_RECORD";
    public static final String INTENT_ACTION_STOP = "com.samsung.microbit.ui.activity.AudioRecorderActivity.action" +
            ".STOP";//close

    @Override
    public void handleEntry(CmdArg cmd) {
        final String audioRecordAction;

        switch(cmd.getCMD()) {
            case EventSubCodes.SAMSUNG_AUDIO_RECORDER_EVT_START_CAPTURE:
                audioRecordAction = INTENT_ACTION_START_RECORD;
                break;

            case EventSubCodes.SAMSUNG_AUDIO_RECORDER_EVT_STOP_CAPTURE:
                audioRecordAction = INTENT_ACTION_STOP_RECORD;
                break;

            case EventSubCodes.SAMSUNG_AUDIO_RECORDER_EVT_LAUNCH:
                audioRecordAction = INTENT_ACTION_LAUNCH;
                break;

            case EventSubCodes.SAMSUNG_AUDIO_RECORDER_EVT_STOP:
                audioRecordAction = INTENT_ACTION_STOP;
                break;

            default:
                Log.e(TAG, "Unknown Event subCode : " + cmd.getCMD());
                return;
        }

        launchActivity(audioRecordAction);
    }

    /**
     * Starts audio recorder activity to proceed audio recording.
     *
     * @param action Action that should be done along with starting the activity.
     */
    private static void launchActivity(String action) {
        Context context = MBApp.getApp();

        Intent mIntent = new Intent(context, AudioRecorderActivity.class);
        mIntent.setAction(action);

        // keep same instance of activity
        mIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        context.startActivity(mIntent);
    }

    @Override
    public void destroy() {

    }
}
