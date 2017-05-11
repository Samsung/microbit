package com.samsung.microbit.presentation;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

import com.samsung.microbit.MBApp;
import com.samsung.microbit.data.model.CmdArg;
import com.samsung.microbit.plugin.InformationPlugin;
import com.samsung.microbit.service.PluginService;

public class TemperaturePresenter implements Presenter {
    /*
     * Temperature listener
     */
    private SensorEventListener temperatureListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            float temperature = event.values[0];

            if(informationPlugin != null) {
                //notify BLE
                CmdArg cmd = new CmdArg(InformationPlugin.AlertType.TYPE_TEMPERATURE, "Temperature " + temperature);
                informationPlugin.sendReplyCommand(PluginService.INFORMATION, cmd);
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }
    };

    private SensorManager sensorManager;
    private InformationPlugin informationPlugin;
    private Sensor temperatureSensor;
    private boolean isRegistered;

    public TemperaturePresenter() {
        sensorManager = (SensorManager) MBApp.getApp().getSystemService(Context.SENSOR_SERVICE);
        temperatureSensor = sensorManager.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE);
    }

    public void setInformationPlugin(InformationPlugin informationPlugin) {
        this.informationPlugin = informationPlugin;
    }

    @Override
    public void start() {
        if(temperatureSensor == null) {
            //no temperature sensor
            return;
        }

        if(!isRegistered) {
            isRegistered = true;

            sensorManager.registerListener(temperatureListener, temperatureSensor, SensorManager.SENSOR_DELAY_NORMAL);

            if(informationPlugin != null) {
                CmdArg cmd = new CmdArg(0, "Registered Temperature.");
                informationPlugin.sendReplyCommand(PluginService.INFORMATION, cmd);
            }
        }
    }

    @Override
    public void stop() {
        if(temperatureSensor == null) {
            return;
        }

        if(isRegistered) {
            sensorManager.unregisterListener(temperatureListener);

            if(informationPlugin != null) {
                CmdArg cmd = new CmdArg(0, "Unregistered Temperature.");
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
