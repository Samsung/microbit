package com.samsung.microbit.presentation;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

import com.samsung.microbit.MBApp;
import com.samsung.microbit.data.constants.EventCategories;
import com.samsung.microbit.data.constants.EventSubCodes;
import com.samsung.microbit.data.constants.IPCConstants;
import com.samsung.microbit.service.IPCService;
import com.samsung.microbit.utils.Utils;

public class ScreenOnOffPresenter implements Presenter {
    private static final String TAG = ScreenOnOffPresenter.class.getSimpleName();

    private BroadcastReceiver screenReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(intent.getAction().equals(Intent.ACTION_SCREEN_OFF)) {
                MBApp application = MBApp.getApp();

                Intent charChangedIntent = new Intent(application, IPCService.class);
                charChangedIntent.putExtra(IPCConstants.INTENT_TYPE, EventCategories.IPC_BLE_NOTIFICATION_CHARACTERISTIC_CHANGED);
                charChangedIntent.putExtra(IPCConstants.INTENT_CHARACTERISTIC_MESSAGE, Utils.makeMicroBitValue
                        (EventCategories.SAMSUNG_DEVICE_INFO_ID, EventSubCodes.SAMSUNG_DEVICE_DISPLAY_OFF));
                application.startService(charChangedIntent);
            } else if(intent.getAction().equals(Intent.ACTION_SCREEN_ON)) {
                MBApp application = MBApp.getApp();

                Intent charChangedIntent = new Intent(application, IPCService.class);
                charChangedIntent.putExtra(IPCConstants.INTENT_TYPE, EventCategories.IPC_BLE_NOTIFICATION_CHARACTERISTIC_CHANGED);
                charChangedIntent.putExtra(IPCConstants.INTENT_CHARACTERISTIC_MESSAGE, Utils.makeMicroBitValue
                        (EventCategories.SAMSUNG_DEVICE_INFO_ID, EventSubCodes.SAMSUNG_DEVICE_DISPLAY_ON));
                application.startService(charChangedIntent);
            }
        }
    };

    private MBApp application;
    private boolean isRegistered;

    public ScreenOnOffPresenter() {
        application = MBApp.getApp();
    }

    @Override
    public void start() {
        if(!isRegistered) {
            isRegistered = true;
            Log.i(TAG, "registerDisplay() ");

            IntentFilter screenStateFilter = new IntentFilter();
            screenStateFilter.addAction(Intent.ACTION_SCREEN_ON);
            screenStateFilter.addAction(Intent.ACTION_SCREEN_OFF);
            application.registerReceiver(screenReceiver, screenStateFilter);
        }
    }

    @Override
    public void stop() {
        if(isRegistered) {
            Log.i(TAG, "unregisterDisplay() ");

            application.unregisterReceiver(screenReceiver);
            isRegistered = false;
        }
    }

    @Override
    public void destroy() {
        stop();
    }
}
