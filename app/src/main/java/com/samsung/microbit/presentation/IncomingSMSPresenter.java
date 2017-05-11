package com.samsung.microbit.presentation;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.provider.Telephony;

import com.samsung.microbit.MBApp;
import com.samsung.microbit.data.constants.EventCategories;
import com.samsung.microbit.data.constants.EventSubCodes;
import com.samsung.microbit.data.constants.IPCConstants;
import com.samsung.microbit.data.model.CmdArg;
import com.samsung.microbit.plugin.TelephonyPlugin;
import com.samsung.microbit.service.IPCService;
import com.samsung.microbit.service.PluginService;
import com.samsung.microbit.utils.Utils;

public class IncomingSMSPresenter implements Presenter {
    private static final String TAG = IncomingSMSPresenter.class.getSimpleName();

    private static class IncomingSMSListener extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(intent.getAction().equals(Telephony.Sms.Intents.SMS_RECEIVED_ACTION)) {
                MBApp application = MBApp.getApp();

                Intent charChangedIntent = new Intent(application, IPCService.class);
                charChangedIntent.putExtra(IPCConstants.INTENT_TYPE, EventCategories.IPC_BLE_NOTIFICATION_CHARACTERISTIC_CHANGED);
                charChangedIntent.putExtra(IPCConstants.INTENT_CHARACTERISTIC_MESSAGE, Utils.makeMicroBitValue
                        (EventCategories.SAMSUNG_DEVICE_INFO_ID, EventSubCodes.SAMSUNG_INCOMING_SMS));
                application.startService(charChangedIntent);
            }
        }
    }

    private MBApp microBitApp;
    private IncomingSMSListener incomingSMSListener = new IncomingSMSListener();
    private TelephonyPlugin telephonyPlugin;

    private boolean isRegistered;

    public IncomingSMSPresenter() {
        microBitApp = MBApp.getApp();
    }

    public void setTelephonyPlugin(TelephonyPlugin telephonyPlugin) {
        this.telephonyPlugin = telephonyPlugin;
    }

    @Override
    public void start() {
        if(!isRegistered) {
            microBitApp.registerReceiver(incomingSMSListener, new IntentFilter(Telephony.Sms.Intents.SMS_RECEIVED_ACTION));

            if(telephonyPlugin != null) {
                CmdArg cmd = new CmdArg(0, "Registered Incoming SMS Alert");
                telephonyPlugin.sendCommandBLE(PluginService.TELEPHONY, cmd);
                //TODO: do we need to report registration status?
            }
            isRegistered = true;
        }
    }

    @Override
    public void stop() {
        if(isRegistered) {
            microBitApp.unregisterReceiver(incomingSMSListener);

            if(telephonyPlugin != null) {
                CmdArg cmd = new CmdArg(0, "Unregistered Incoming SMS Alert");
                telephonyPlugin.sendCommandBLE(PluginService.TELEPHONY, cmd);
                //TODO: do we need to report registration status?
            }
            isRegistered = false;
        }
    }

    @Override
    public void destroy() {
        stop();
    }
}
