package com.samsung.microbit.plugin;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

import com.samsung.microbit.data.constants.RegistrationIds;
import com.samsung.microbit.data.model.CmdArg;
import com.samsung.microbit.presentation.IncomingCallPresenter;
import com.samsung.microbit.presentation.IncomingSMSPresenter;

/**
 * Allows to share statistic about incoming telephone calls and sms on a mobile device.
 */
public class TelephonyPlugin implements AbstractPlugin {
    private static final String TAG = TelephonyPlugin.class.getSimpleName();

    private IncomingCallPresenter incomingCallPresenter;
    private IncomingSMSPresenter incomingSMSPresenter;

    private boolean callPresenterInited;
    private boolean smsPresenterInited;

    private final Handler serviceHandler;

    public TelephonyPlugin(Handler serviceHandler) {
        this.serviceHandler = serviceHandler;
    }

    @Override
    public void handleEntry(CmdArg cmd) {
        boolean register = false;
        if(cmd.getValue() != null) {
            register = cmd.getValue().toLowerCase().equals("on");
        }

        switch(cmd.getCMD()) {
            case RegistrationIds.REG_TELEPHONY: {
                if(register) {
                    if(incomingCallPresenter == null) {
                        incomingCallPresenter = new IncomingCallPresenter();
                    }

                    if(!callPresenterInited) {
                        incomingCallPresenter.setTelephonyPlugin(this);
                        callPresenterInited = true;
                    }

                    incomingCallPresenter.start();
                } else {
                    if(incomingCallPresenter != null) {
                        incomingCallPresenter.stop();
                    }
                }
                break;
            }
            case RegistrationIds.REG_MESSAGING: {
                if(register) {
                    if(incomingSMSPresenter == null) {
                        incomingSMSPresenter = new IncomingSMSPresenter();
                    }

                    if(!smsPresenterInited) {
                        incomingSMSPresenter.setTelephonyPlugin(this);
                        smsPresenterInited = true;
                    }

                    incomingSMSPresenter.start();
                } else {
                    if(incomingSMSPresenter != null) {
                        incomingSMSPresenter.stop();
                    }
                }
                break;
            }
        }
    }

    @Override
    public void destroy() {
        if(incomingCallPresenter != null) {
            incomingCallPresenter.stop();
            incomingCallPresenter.setTelephonyPlugin(null);
            incomingCallPresenter.destroy();
            callPresenterInited = false;
        }

        if(incomingSMSPresenter != null) {
            incomingSMSPresenter.stop();
            incomingSMSPresenter.setTelephonyPlugin(null);
            incomingSMSPresenter.destroy();
            smsPresenterInited = false;
        }
    }

    public void sendCommandBLE(int mbsService, CmdArg cmd) {
        Message msg = Message.obtain(null, mbsService);
        Bundle bundle = new Bundle();
        bundle.putInt("cmd", cmd.getCMD());
        bundle.putString("value", cmd.getValue());
        msg.setData(bundle);
        serviceHandler.sendMessage(msg);
    }
}
