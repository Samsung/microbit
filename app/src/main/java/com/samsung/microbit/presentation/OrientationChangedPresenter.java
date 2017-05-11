package com.samsung.microbit.presentation;

import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;

import com.samsung.microbit.MBApp;
import com.samsung.microbit.data.constants.EventCategories;
import com.samsung.microbit.data.constants.EventSubCodes;
import com.samsung.microbit.data.constants.IPCConstants;
import com.samsung.microbit.service.IPCService;
import com.samsung.microbit.utils.Utils;

public class OrientationChangedPresenter implements Presenter {

    private static final float Y_DELTA_FOR_DETECT_LANDSCAPE = 6.5f;

    private static final String TAG = OrientationChangedPresenter.class.getSimpleName();

    private SensorEventListener orientationListener = new SensorEventListener() {
        int orientation = -1;

        @Override
        public void onSensorChanged(SensorEvent event) {
            if(Math.abs(event.values[1]) < Y_DELTA_FOR_DETECT_LANDSCAPE) {
                if(orientation != EventSubCodes.SAMSUNG_DEVICE_ORIENTATION_LANDSCAPE) {
                    Log.d(TAG, "Landscape");
                }
                orientation = EventSubCodes.SAMSUNG_DEVICE_ORIENTATION_LANDSCAPE;
            } else {
                if(orientation != EventSubCodes.SAMSUNG_DEVICE_ORIENTATION_PORTRAIT) {
                    Log.d(TAG, "Portrait");
                }
                orientation = EventSubCodes.SAMSUNG_DEVICE_ORIENTATION_PORTRAIT;
            }

            if(previousOrientation != orientation) {
                MBApp application = MBApp.getApp();

                Intent intent = new Intent(application, IPCService.class);
                intent.putExtra(IPCConstants.INTENT_TYPE, EventCategories.IPC_BLE_NOTIFICATION_CHARACTERISTIC_CHANGED);
                intent.putExtra(IPCConstants.INTENT_CHARACTERISTIC_MESSAGE, Utils.makeMicroBitValue
                        (EventCategories.SAMSUNG_DEVICE_INFO_ID, orientation));
                application.startService(intent);

                previousOrientation = orientation;
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {

        }
    };

    private int previousOrientation = -1;
    private SensorManager sensorManager;
    private boolean isRegistered;

    public OrientationChangedPresenter() {
        sensorManager = (SensorManager) MBApp.getApp().getSystemService(Context.SENSOR_SERVICE);
    }

    @Override
    public void start() {
        if(!isRegistered) {
            sensorManager.registerListener(orientationListener, sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                    SensorManager.SENSOR_DELAY_NORMAL);
            isRegistered = true;
        }
    }

    @Override
    public void stop() {
        if(isRegistered) {
            sensorManager.unregisterListener(orientationListener);
            isRegistered = false;
        }
    }

    @Override
    public void destroy() {
        stop();
    }
}
