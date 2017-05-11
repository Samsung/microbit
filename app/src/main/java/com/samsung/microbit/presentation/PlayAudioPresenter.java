package com.samsung.microbit.presentation;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.content.res.Resources;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.util.Log;

import com.samsung.microbit.MBApp;

import java.io.IOException;

/**
 * Provides methods to manage a media player and play raw audio files.
 */
public class PlayAudioPresenter implements Presenter {
    private static final String TAG = PlayAudioPresenter.class.getSimpleName();

    private AudioManager audioManager;

    private int originalRingerMode;
    private int originalRingerVolume;

    private String internalPath;
    private MediaPlayer mediaplayer;
    private MediaPlayer.OnCompletionListener callBack;

    public PlayAudioPresenter() {
    }

    public void setInternalPathForPlay(String rawNameForPlay) {
        this.internalPath = rawNameForPlay;
    }

    public void setCallBack(MediaPlayer.OnCompletionListener callBack) {
        this.callBack = callBack;
    }

    @Override
    public void start() {
        MBApp app = MBApp.getApp();

        Resources resources = app.getResources();
        int resID = resources.getIdentifier(internalPath, "raw", app.getPackageName());
        AssetFileDescriptor afd = resources.openRawResourceFd(resID);

        preparePhoneToPlayAudio(app);

        if(mediaplayer == null) {
            mediaplayer = new MediaPlayer();
        }

        mediaplayer.reset();
        mediaplayer.setAudioStreamType(AudioManager.STREAM_NOTIFICATION);
        try {
            mediaplayer.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
            afd.close();
            mediaplayer.prepare();
        } catch(IOException e) {
            e.printStackTrace();
            Log.e(TAG, e.toString());
            mediaplayer.release();
            mediaplayer = null;
            return;
        }
        //Set a callback for completion
        mediaplayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                restoreAudioMode();
                if(callBack != null) {
                    callBack.onCompletion(mp);
                }
                mediaplayer.release();
                mediaplayer = null;
            }
        });
        mediaplayer.start();
    }

    private void preparePhoneToPlayAudio(Context context) {
        audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        originalRingerMode = audioManager.getRingerMode();

        originalRingerMode = audioManager.getRingerMode();
        originalRingerVolume = audioManager.getStreamVolume(AudioManager.STREAM_NOTIFICATION);

        if(originalRingerMode != AudioManager.RINGER_MODE_NORMAL) {
            audioManager.setRingerMode(AudioManager.RINGER_MODE_NORMAL);
        }
        audioManager.setStreamVolume(AudioManager.STREAM_NOTIFICATION, audioManager.getStreamMaxVolume
                (AudioManager.STREAM_NOTIFICATION), 0);
    }

    private void restoreAudioMode() {
        audioManager.setRingerMode(originalRingerMode);
        audioManager.setStreamVolume(AudioManager.STREAM_NOTIFICATION, originalRingerVolume, 0);
    }

    @Override
    public void stop() {
        if(mediaplayer != null) {
            try {
                if(mediaplayer.isPlaying()) {
                    mediaplayer.stop();
                    restoreAudioMode();
                }
            } catch(IllegalStateException e) {
                Log.e(TAG, e.toString());
            }
        }
    }

    @Override
    public void destroy() {
        if(mediaplayer != null) {
            stop();
            mediaplayer.release();
            mediaplayer = null;
        }
    }
}