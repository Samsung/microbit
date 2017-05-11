package com.samsung.microbit.plugin;

import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.PowerManager;
import android.util.Log;

import com.samsung.microbit.MBApp;
import com.samsung.microbit.data.constants.EventSubCodes;
import com.samsung.microbit.data.constants.InternalPaths;
import com.samsung.microbit.data.model.CmdArg;
import com.samsung.microbit.presentation.PlayAudioPresenter;
import com.samsung.microbit.ui.activity.CameraActivityPermissionChecker;

/**
 * Allows to start and interact with a device camera.
 */
public class CameraPlugin implements AbstractPlugin {
    private static final String TAG = CameraPlugin.class.getSimpleName();

    public static final String OPEN_FOR_PIC_ACTION = "OPEN_FOR_PIC";
    public static final String OPEN_FOR_VIDEO_ACTION = "OPEN_FOR_VIDEO";

    public static final String ACTION_TAKE_PICTURE = "TAKE_PIC";
    public static final String ACTION_TAKE_VIDEO = "START_VIDEO";
    public static final String ACTION_TOGGLE_CAMERA = "TOGGLE_CAMERA";
    public static final String ACTION_STOP_VIDEO = "STOP_VIDEO";
    public static final String ACTION_CLOSE = "CLOSE";

    private PowerManager mPowerManager;
    private PowerManager.WakeLock mWakeLock;

    private int m_CurrentState;
    private int m_NextState;
    private PlayAudioPresenter playAudioPresenter;

    private MediaPlayer.OnCompletionListener onCompletionListener = new MediaPlayer.OnCompletionListener() {
        @Override
        public void onCompletion(MediaPlayer mp) {
            performOnEnd();
        }
    };

    @Override
    public void handleEntry(CmdArg cmd) {
        Context ctx = MBApp.getApp();

        if(mPowerManager == null) {
            mPowerManager = (PowerManager) ctx.getApplicationContext().getSystemService(Context.POWER_SERVICE);
            mWakeLock = mPowerManager.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP, TAG);
        }

        int cmdArg = cmd.getCMD();

        if(cmdArg == EventSubCodes.SAMSUNG_CAMERA_EVT_STOP_PHOTO_MODE || cmdArg == EventSubCodes
                .SAMSUNG_CAMERA_EVT_STOP_VIDEO_MODE) {
            Intent intent = new Intent(ACTION_CLOSE);
            ctx.sendBroadcast(intent);
            return;
        }

        mWakeLock.acquire(5 * 1000);

        switch(cmd.getCMD()) {
            case EventSubCodes.SAMSUNG_CAMERA_EVT_LAUNCH_PHOTO_MODE:
                m_CurrentState = EventSubCodes.SAMSUNG_CAMERA_EVT_LAUNCH_PHOTO_MODE;
                m_NextState = EventSubCodes.SAMSUNG_CAMERA_EVT_LAUNCH_PHOTO_MODE;

                if(playAudioPresenter != null) {
                    playAudioPresenter.stop();
                } else {
                    playAudioPresenter = new PlayAudioPresenter();
                }

                playAudioPresenter.setInternalPathForPlay(InternalPaths.LAUNCH_CAMERA_AUDIO_PHOTO);
                playAudioPresenter.setCallBack(onCompletionListener);
                playAudioPresenter.start();
                break;

            case EventSubCodes.SAMSUNG_CAMERA_EVT_LAUNCH_VIDEO_MODE:
                m_CurrentState = EventSubCodes.SAMSUNG_CAMERA_EVT_LAUNCH_VIDEO_MODE;
                m_NextState = EventSubCodes.SAMSUNG_CAMERA_EVT_LAUNCH_VIDEO_MODE;

                if(playAudioPresenter != null) {
                    playAudioPresenter.stop();
                } else {
                    playAudioPresenter = new PlayAudioPresenter();
                }

                playAudioPresenter.setInternalPathForPlay(InternalPaths.LAUNCH_CAMERA_AUDIO_VIDEO);
                playAudioPresenter.setCallBack(onCompletionListener);
                playAudioPresenter.start();
                break;

            case EventSubCodes.SAMSUNG_CAMERA_EVT_TAKE_PHOTO:
                m_NextState = EventSubCodes.SAMSUNG_CAMERA_EVT_TAKE_PHOTO;
                performOnEnd();
                break;

            case EventSubCodes.SAMSUNG_CAMERA_EVT_START_VIDEO_CAPTURE:
                m_NextState = EventSubCodes.SAMSUNG_CAMERA_EVT_START_VIDEO_CAPTURE;
                performOnEnd();
                break;

            case EventSubCodes.SAMSUNG_CAMERA_EVT_STOP_VIDEO_CAPTURE:
                recVideoStop();
                break;

            case EventSubCodes.SAMSUNG_CAMERA_EVT_TOGGLE_FRONT_REAR:
                toggleCamera();
                break;
            default:
                Log.e(TAG, "Unknown Event subCode : " + cmd.getCMD());
                break;
        }
    }

    @Override
    public void destroy() {
        if(playAudioPresenter != null) {
            playAudioPresenter.stop();
            playAudioPresenter.setCallBack(null);
            playAudioPresenter = null;
        }
    }

    /**
     * Provides some actions needed to perform at the end.
     */
    private void performOnEnd() {
        Log.d(TAG, "Next state - " + m_NextState);
        switch(m_NextState) {
            case EventSubCodes.SAMSUNG_CAMERA_EVT_LAUNCH_PHOTO_MODE:
                launchCamera(true);
                break;
            case EventSubCodes.SAMSUNG_CAMERA_EVT_LAUNCH_VIDEO_MODE:
                launchCamera(false);
                break;
            case EventSubCodes.SAMSUNG_CAMERA_EVT_TAKE_PHOTO:
                takePic();
                break;
            case EventSubCodes.SAMSUNG_CAMERA_EVT_START_VIDEO_CAPTURE:
                recVideoStart();
                break;
        }
    }

    /**
     * Sends a broadcast intent to take a picture.
     */
    private static void takePic() {
        Intent intent = new Intent(ACTION_TAKE_PICTURE);
        MBApp.getApp().sendBroadcast(intent);
    }

    /**
     * Sends a broadcast intent to start video recording.
     */
    private static void recVideoStart() {
        Intent intent = new Intent(ACTION_TAKE_VIDEO);
        MBApp.getApp().sendBroadcast(intent);
    }

    /**
     * Sends a broadcast intent to stop video recording.
     */
    private static void recVideoStop() {
        Intent intent = new Intent(ACTION_STOP_VIDEO);
        MBApp.getApp().sendBroadcast(intent);
    }

    /**
     * Sends a broadcast intent to turn on/off a camera.
     */
    private static void toggleCamera() {
        Intent intent = new Intent(ACTION_TOGGLE_CAMERA);
        MBApp.getApp().sendBroadcast(intent);
    }

    /**
     * Starts activity to take a picture or to record a video.
     */
    private void launchCamera(boolean launchForPic) {
        Context context = MBApp.getApp();

        Intent mIntent = new Intent(context, CameraActivityPermissionChecker.class);
        mIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        if(launchForPic) {
            mIntent.setAction(OPEN_FOR_PIC_ACTION);
        } else {
            mIntent.setAction(OPEN_FOR_VIDEO_ACTION);
        }

        context.startActivity(mIntent);
    }
}
