package com.samsung.microbit.presentation;

import android.content.Context;
import android.os.Vibrator;

import com.samsung.microbit.MBApp;

public class VibratePresenter implements Presenter {
    private long vibrateTimeMillis;
    private Vibrator vibrator;
    private boolean startVibration;

    public VibratePresenter() {
    }

    public void reInit(long vibrateTimeMillis) {
        this.vibrateTimeMillis = vibrateTimeMillis;
        this.vibrator = (Vibrator) MBApp.getApp().getSystemService(Context.VIBRATOR_SERVICE);
    }

    @Override
    public void start() {
        if(vibrator != null && vibrator.hasVibrator()) {
            startVibration = true;
            vibrator.vibrate(vibrateTimeMillis);
        } else {
            startVibration = false;
        }
    }

    @Override
    public void stop() {
        if(startVibration) {
            vibrator.cancel();
        }
    }

    @Override
    public void destroy() {
        stop();
        vibrator = null;
    }
}
