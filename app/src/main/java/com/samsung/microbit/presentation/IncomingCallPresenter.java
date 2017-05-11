package com.samsung.microbit.presentation;

import android.content.Context;
import android.content.Intent;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.samsung.microbit.MBApp;
import com.samsung.microbit.data.constants.EventCategories;
import com.samsung.microbit.data.constants.EventSubCodes;
import com.samsung.microbit.data.constants.IPCConstants;
import com.samsung.microbit.data.model.CmdArg;
import com.samsung.microbit.plugin.TelephonyPlugin;
import com.samsung.microbit.service.IPCService;
import com.samsung.microbit.service.PluginService;
import com.samsung.microbit.utils.Utils;

public class IncomingCallPresenter implements Presenter {
    private static final String TAG = IncomingCallPresenter.class.getSimpleName();

    private PhoneStateListener incomingCallListener = new PhoneStateListener() {
        @Override
        public void onCallStateChanged(int state, String incomingNumber) {
            switch(state) {
                case TelephonyManager.CALL_STATE_RINGING:
                    Log.i(TAG, "onCallStateChanged: " + state);

                    MBApp application = MBApp.getApp();

                    Intent intent = new Intent(application, IPCService.class);
                    intent.putExtra(IPCConstants.INTENT_TYPE, EventCategories.IPC_BLE_NOTIFICATION_CHARACTERISTIC_CHANGED);
                    intent.putExtra(IPCConstants.INTENT_CHARACTERISTIC_MESSAGE, Utils.makeMicroBitValue
                            (EventCategories.SAMSUNG_DEVICE_INFO_ID, EventSubCodes.SAMSUNG_INCOMING_CALL));
                    application.startService(intent);
                    break;
            }
        }
    };

    private TelephonyManager telephonyManager;
    private boolean isRegistered;
    private TelephonyPlugin telephonyPlugin;

    public IncomingCallPresenter() {
        telephonyManager = (TelephonyManager) MBApp.getApp().getSystemService(Context.TELEPHONY_SERVICE);
    }

    public void setTelephonyPlugin(TelephonyPlugin telephonyPlugin) {
        this.telephonyPlugin = telephonyPlugin;
    }

    @Override
    public void start() {
        if(!isRegistered) {
            isRegistered = true;
            telephonyManager.listen(incomingCallListener, PhoneStateListener.LISTEN_CALL_STATE);

            if(telephonyPlugin != null) {
                CmdArg cmd = new CmdArg(0, "Registered Incoming Call Alert");
                telephonyPlugin.sendCommandBLE(PluginService.TELEPHONY, cmd);//TODO: do we need to report
                // registration status?
            }
        }
    }

    @Override
    public void stop() {
        if(isRegistered) {
            telephonyManager.listen(incomingCallListener, TelephonyManager.PHONE_TYPE_NONE);

            if(telephonyPlugin != null) {
                CmdArg cmd = new CmdArg(0, "Unregistered Incoming Call Alert");
                telephonyPlugin.sendCommandBLE(PluginService.TELEPHONY, cmd);//TODO: do we need to report
                // registration status?
            }

            isRegistered = false;
        }
    }

    @Override
    public void destroy() {
        stop();
    }
}
