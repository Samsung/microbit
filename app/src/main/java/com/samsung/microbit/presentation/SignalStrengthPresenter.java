package com.samsung.microbit.presentation;

import android.content.Context;
import android.content.Intent;
import android.telephony.PhoneStateListener;
import android.telephony.SignalStrength;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.samsung.microbit.MBApp;
import com.samsung.microbit.data.constants.EventCategories;
import com.samsung.microbit.data.constants.EventSubCodes;
import com.samsung.microbit.data.constants.IPCConstants;
import com.samsung.microbit.data.model.CmdArg;
import com.samsung.microbit.plugin.InformationPlugin;
import com.samsung.microbit.service.IPCService;
import com.samsung.microbit.service.PluginService;
import com.samsung.microbit.utils.Utils;

public class SignalStrengthPresenter implements Presenter {
    private static final String TAG = SignalStrengthPresenter.class.getSimpleName();

    private PhoneStateListener phoneListener = new PhoneStateListener() {
        @Override
        public void onSignalStrengthsChanged(SignalStrength signalStrength) {
            Log.i(TAG, "onSignalStrengthsChanged: ");

            updateSignalStrength(signalStrength);
        }
    };

    private int sCurrentSignalStrength;
    private TelephonyManager telephonyManager;
    private boolean isRegistered;
    private InformationPlugin informationPlugin;

    public SignalStrengthPresenter() {
        telephonyManager = (TelephonyManager) MBApp.getApp().getSystemService(Context.TELEPHONY_SERVICE);
    }

    public void setInformationPlugin(InformationPlugin informationPlugin) {
        this.informationPlugin = informationPlugin;
    }

    @Override
    public void start() {
        if(!isRegistered) {
            isRegistered = true;
            Log.i(TAG, "registerSignalStrength");
            telephonyManager.listen(phoneListener, PhoneStateListener.LISTEN_SIGNAL_STRENGTHS);

            if(informationPlugin != null) {
                CmdArg cmd = new CmdArg(0, "Registered Signal Strength.");
                informationPlugin.sendReplyCommand(PluginService.INFORMATION, cmd);
            }
        }
    }

    @Override
    public void stop() {
        if(isRegistered) {
            telephonyManager.listen(phoneListener, PhoneStateListener.LISTEN_NONE);

            if(informationPlugin != null) {
                CmdArg cmd = new CmdArg(0, "Unregistered Signal Strength.");
                informationPlugin.sendReplyCommand(PluginService.INFORMATION, cmd);
            }

            isRegistered = false;
        }
    }

    @Override
    public void destroy() {
        stop();
    }

    private void updateSignalStrength(SignalStrength signalStrength) {
        final int level;
        Log.i(TAG, "updateSignalStrength: ");
        if(!isCdma(signalStrength)) {
            int asu = signalStrength.getGsmSignalStrength();
            // ASU ranges from 0 to 31 - TS 27.007 Sec 8.5
            // asu = 0 (-113dB or less) is very weak
            // signal, its better to show 0 bars to the user in such cases.
            // asu = 99 is a special case, where the signal strength is unknown.
            if(asu <= 2 || asu == 99) level = EventSubCodes.SAMSUNG_SIGNAL_STRENGTH_EVT_NO_BAR;
            else if(asu >= 12) level = EventSubCodes.SAMSUNG_SIGNAL_STRENGTH_EVT_FOUR_BAR;
            else if(asu >= 8) level = EventSubCodes.SAMSUNG_SIGNAL_STRENGTH_EVT_THREE_BAR;
            else if(asu >= 5) level = EventSubCodes.SAMSUNG_SIGNAL_STRENGTH_EVT_TWO_BAR;
            else level = EventSubCodes.SAMSUNG_SIGNAL_STRENGTH_EVT_ONE_BAR;
        } else {
            level = getCdmaLevel(signalStrength);
        }

        if(level != sCurrentSignalStrength) {
            sCurrentSignalStrength = level;

            MBApp application = MBApp.getApp();

            Intent intent = new Intent(application, IPCService.class);
            intent.putExtra(IPCConstants.INTENT_TYPE, EventCategories.IPC_BLE_NOTIFICATION_CHARACTERISTIC_CHANGED);
            intent.putExtra(IPCConstants.INTENT_CHARACTERISTIC_MESSAGE, Utils.makeMicroBitValue
                    (EventCategories.SAMSUNG_SIGNAL_STRENGTH_ID, level));
            application.startService(intent);
        }
    }

    private int getCdmaLevel(SignalStrength signalStrength) {
        final int cdmaDbm = signalStrength.getCdmaDbm();
        final int cdmaEcio = signalStrength.getCdmaEcio();

        final int levelDbm;
        if(cdmaDbm >= -75) levelDbm = 4;
        else if(cdmaDbm >= -85) levelDbm = 3;
        else if(cdmaDbm >= -95) levelDbm = 2;
        else if(cdmaDbm >= -100) levelDbm = 1;
        else levelDbm = 0;

        final int levelEcio;
        // Ec/Io are in dB*10
        if(cdmaEcio >= -90) levelEcio = 4;
        else if(cdmaEcio >= -110) levelEcio = 3;
        else if(cdmaEcio >= -130) levelEcio = 2;
        else if(cdmaEcio >= -150) levelEcio = 1;
        else levelEcio = 0;

        return (levelDbm < levelEcio) ? levelDbm : levelEcio;
    }

    private boolean isCdma(SignalStrength signalStrength) {
        return (signalStrength != null) && !signalStrength.isGsm();
    }

}
