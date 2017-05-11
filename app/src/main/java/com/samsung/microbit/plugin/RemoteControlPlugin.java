package com.samsung.microbit.plugin;

import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.util.Log;
import android.view.KeyEvent;

import com.samsung.microbit.MBApp;
import com.samsung.microbit.data.constants.EventSubCodes;
import com.samsung.microbit.data.model.CmdArg;

import java.util.Timer;
import java.util.TimerTask;

import static com.samsung.microbit.BuildConfig.DEBUG;

/**
 * Provides remote control of a media player.
 * For example, it can start a music player on your phone and control playback
 * using keys on a micro:bit board.
 */
public class RemoteControlPlugin implements AbstractPlugin {
    private static final String TAG = RemoteControlPlugin.class.getSimpleName();

    /**
     * Simplified method to log informational messages.
     *
     * @param message Message to log.
     */
    private static void logi(String message) {
        Log.i(TAG, "### " + Thread.currentThread().getId() + " # " + message);
    }

    private Timer timer;

    @Override
    public void handleEntry(CmdArg cmd) {
        if(DEBUG) {
            logi("pluginEntry() ##  " + cmd.getCMD());
        }

        if(timer == null) {
            timer = new Timer();
        }

        switch(cmd.getCMD()) {
            case EventSubCodes.SAMSUNG_REMOTE_CONTROL_EVT_PLAY:
                play();
                break;

            case EventSubCodes.SAMSUNG_REMOTE_CONTROL_EVT_PAUSE:
                pause();
                break;

            case EventSubCodes.SAMSUNG_REMOTE_CONTROL_EVT_STOP:
                stopPlaying();
                break;

            case EventSubCodes.SAMSUNG_REMOTE_CONTROL_EVT_NEXTTRACK:
                nextTrack();
                break;

            case EventSubCodes.SAMSUNG_REMOTE_CONTROL_EVT_PREVTRACK:
                previousTrack();
                break;

            case EventSubCodes.SAMSUNG_REMOTE_CONTROL_EVT_FORWARD:
                forward();
                break;

            case EventSubCodes.SAMSUNG_REMOTE_CONTROL_EVT_REWIND:
                rewind();
                break;

            case EventSubCodes.SAMSUNG_REMOTE_CONTROL_EVT_VOLUMEUP:
                volumeUp();
                break;

            case EventSubCodes.SAMSUNG_REMOTE_CONTROL_EVT_VOLUMEDOWN:
                volumeDown();
                break;
            default:
                Log.e(TAG, "Unknown Event subCode : " + cmd.getCMD());
        }
    }

    /**
     * General method to send media key event with provides event code and action to perform.
     *
     * @param action Key event action.
     * @param code   Event code.
     */
    private void sendMediaKeyEvent(final int action, final int code) {
        Intent mediaEvent = new Intent(Intent.ACTION_MEDIA_BUTTON);
        KeyEvent event = new KeyEvent(action, code);
        mediaEvent.putExtra(Intent.EXTRA_KEY_EVENT, event);
        MBApp.getApp().sendOrderedBroadcast(mediaEvent, null);
    }

    /**
     * Provides schedule to send media key event.
     *
     * @param action Event action.
     * @param code   Event code.
     * @param delay  Schedule delay.
     */
    private void scheduleMediaKeyEvent(final int action, final int code, final int delay) {
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                sendMediaKeyEvent(action, code);
            }
        }, delay);
    }

    private void play() {
        scheduleMediaKeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PLAY, 0);
        scheduleMediaKeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_PLAY, 100);
    }

    private void pause() {
        scheduleMediaKeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PAUSE, 0);
        scheduleMediaKeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_PAUSE, 100);
    }

    private void stopPlaying() {
        scheduleMediaKeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_STOP, 0);
        scheduleMediaKeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_STOP, 100);
    }

    private void nextTrack() {
        scheduleMediaKeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_NEXT, 0);
        scheduleMediaKeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_NEXT, 100);
    }

    private void previousTrack() {
        scheduleMediaKeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PREVIOUS, 0);
        scheduleMediaKeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_PREVIOUS, 100);
    }

    private void forward() {
        scheduleMediaKeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_FAST_FORWARD, 0);
        scheduleMediaKeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_FAST_FORWARD, 100);
    }

    private void rewind() {
        scheduleMediaKeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_REWIND, 0);
        scheduleMediaKeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_REWIND, 100);
    }

    private void volumeUp() {
        AudioManager audio = (AudioManager) MBApp.getApp().getSystemService(Context.AUDIO_SERVICE);
        audio.adjustStreamVolume(AudioManager.STREAM_MUSIC,
                AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI);
        /*
        scheduleMediaKeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_VOLUME_UP, 0);
        scheduleMediaKeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_VOLUME_UP, 100);
        */
    }

    private void volumeDown() {
        AudioManager audio = (AudioManager) MBApp.getApp().getSystemService(Context.AUDIO_SERVICE);
        audio.adjustStreamVolume(AudioManager.STREAM_MUSIC,
                AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI);
		/*
        scheduleMediaKeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_VOLUME_DOWN, 0);
        scheduleMediaKeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_VOLUME_DOWN, 100);
        */
    }

    @Override
    public void destroy() {
        if(timer != null) {
            timer.cancel();
            timer = null;
        }
    }
}
