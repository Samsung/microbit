package com.samsung.microbit.presentation;

import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;

import com.samsung.microbit.MBApp;
import com.samsung.microbit.plugin.AlertPlugin;

import java.util.Timer;
import java.util.TimerTask;

public class PlayRingtonePresenter implements Presenter {
    private static final String TAG = PlayRingtonePresenter.class.getSimpleName();

    private MediaPlayer mediaPlayer;
    private Uri ringtoneUri;
    private int maxDuration;
    private boolean isAlarm;

    private Ringtone ringtone;
    private Timer timer;

    public PlayRingtonePresenter() {

    }

    public void reInit(MediaPlayer mediaPlayer, Uri ringtoneUri, int maxDuration, boolean isAlarm) {
        this.mediaPlayer = mediaPlayer;
        this.ringtoneUri = ringtoneUri;
        this.maxDuration = maxDuration;
        this.isAlarm = isAlarm;
    }

    @Override
    public void start() {
        int duration = AlertPlugin.getDuration(mediaPlayer, ringtoneUri);

        if(maxDuration > 0 && duration > maxDuration) {
            duration = maxDuration;
        }

        if(ringtone != null && ringtone.isPlaying()) {
            ringtone.stop();
        }

        if(timer != null) {
            //After this operation the timer cannot be used anymore
            timer.cancel();
        }

        timer = new Timer();

        ringtone = RingtoneManager.getRingtone(MBApp.getApp(), ringtoneUri);

        if(isAlarm) {
            ringtone.setStreamType(AudioManager.STREAM_ALARM);
        }

        ringtone.play();

        TimerTask stopTask = new TimerTask() {
            @Override
            public void run() {
                stop();
            }
        };

        timer.schedule(stopTask, duration);
    }

    @Override
    public void stop() {
        if(ringtone != null && ringtone.isPlaying()) {
            ringtone.stop();
            ringtone = null;

            timer.cancel();
            timer = null;
        }
    }

    @Override
    public void destroy() {
        stop();

        if(timer != null) {
            timer.cancel();
            timer = null;
        }
    }
}
