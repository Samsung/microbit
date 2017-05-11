package com.samsung.microbit.presentation;

import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

import com.samsung.microbit.MBApp;
import com.samsung.microbit.data.constants.EventCategories;
import com.samsung.microbit.data.constants.EventSubCodes;
import com.samsung.microbit.data.constants.IPCConstants;
import com.samsung.microbit.data.model.CmdArg;
import com.samsung.microbit.plugin.InformationPlugin;
import com.samsung.microbit.service.IPCService;
import com.samsung.microbit.service.PluginService;
import com.samsung.microbit.utils.Utils;

public class ShakePresenter implements Presenter {
    private static final String TAG = ShakePresenter.class.getSimpleName();

    /*
     * ShakeEventListener
     */
    private SensorEventListener shakeEventListener = new SensorEventListener() {
        static final int THRESHOLD_SWING_COUNT = 3;//nb of times swing must be detected before we call it a shake event
        static final int SWING_EVENT_INTERVAL = 100;
        static final int SPEED_THRESHOLD = 500;
        int mSwingCount;
        long lastTime;
        float speed;
        float x, y, z;
        float lastX;
        float lastY;
        float lastZ;

        @Override
        public void onSensorChanged(SensorEvent event) {
            long currentTime = System.currentTimeMillis();
            long deltaTime = currentTime - lastTime;

            if(deltaTime > SWING_EVENT_INTERVAL) {
                lastTime = currentTime;

                x = event.values[0];
                y = event.values[1];
                z = event.values[2];

                speed = Math.abs(x + y + z - lastX - lastY - lastZ) / deltaTime * 10000;

                if(speed > SPEED_THRESHOLD) {
                    mSwingCount++;
                    if(mSwingCount >= THRESHOLD_SWING_COUNT) {
                        if(informationPlugin != null) {
                            //notify BLE client
                            CmdArg cmd = new CmdArg(InformationPlugin.AlertType.TYPE_SHAKE, "Device Shaked");
                            informationPlugin.sendReplyCommand(PluginService.INFORMATION, cmd);
                        }

                        MBApp application = MBApp.getApp();

                        Intent intent = new Intent(application, IPCService.class);
                        intent.putExtra(IPCConstants.INTENT_TYPE, EventCategories.IPC_BLE_NOTIFICATION_CHARACTERISTIC_CHANGED);
                        intent.putExtra(IPCConstants.INTENT_CHARACTERISTIC_MESSAGE, Utils
                                .makeMicroBitValue(EventCategories.SAMSUNG_DEVICE_INFO_ID,
                                        EventSubCodes.SAMSUNG_DEVICE_GESTURE_DEVICE_SHAKEN));
                        application.startService(intent);

                        mSwingCount = 0;
                    }
                } else {
                    mSwingCount = 0;
                    //PluginService.sendMessageToBle(Constants.makeMicroBitValue(Constants.SAMSUNG_DEVICE_INFO_ID, Constants.SAMSUNG_DEVICE_GESTURE_NONE));
                }

                lastX = x;
                lastY = y;
                lastZ = z;
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }
    };

    private InformationPlugin informationPlugin;
    private SensorManager sensorManager;
    private boolean isRegistered;

    public ShakePresenter() {
        sensorManager = (SensorManager) MBApp.getApp().getSystemService(Context.SENSOR_SERVICE);
    }

    public void setInformationPlugin(InformationPlugin informationPlugin) {
        this.informationPlugin = informationPlugin;
    }

    @Override
    public void start() {
        if(!isRegistered) {
            isRegistered = true;
            sensorManager.registerListener(shakeEventListener, sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                    SensorManager.SENSOR_DELAY_NORMAL);

            if(informationPlugin != null) {
                CmdArg cmd = new CmdArg(0, "Registered Shake.");
                informationPlugin.sendReplyCommand(PluginService.INFORMATION, cmd);
            }
        }
    }

    @Override
    public void stop() {
        if(isRegistered) {
            sensorManager.unregisterListener(shakeEventListener);

            if(informationPlugin != null) {
                CmdArg cmd = new CmdArg(0, "Unregistered Shake.");
                informationPlugin.sendReplyCommand(PluginService.INFORMATION, cmd);
            }

            isRegistered = false;
        }
    }

    @Override
    public void destroy() {
        stop();
    }
}
