package com.samsung.microbit.service;

import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.support.annotation.Nullable;
import android.util.Log;

import com.samsung.microbit.data.constants.EventCategories;
import com.samsung.microbit.data.constants.EventSubCodes;
import com.samsung.microbit.data.constants.IPCConstants;
import com.samsung.microbit.data.model.CmdArg;
import com.samsung.microbit.plugin.AbstractPlugin;
import com.samsung.microbit.plugin.PluginsCreator;

import java.lang.ref.WeakReference;

import static com.samsung.microbit.BuildConfig.DEBUG;

public class PluginService extends Service {

    private static final String TAG = PluginService.class.getSimpleName();

    //MBS Services
    public static final int ALERT = 0;
    public static final int FEEDBACK = 1;
    public static final int INFORMATION = 2;
    public static final int AUDIO = 3;
    public static final int REMOTE_CONTROL = 4;
    public static final int TELEPHONY = 5;
    public static final int CAMERA = 6;
    public static final int FILE = 7;

    private static final class PluginHandler extends Handler {
        private final WeakReference<PluginService> pluginServiceWeakReference;

        private PluginHandler(PluginService pluginService) {
            super();
            pluginServiceWeakReference = new WeakReference<>(pluginService);
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);

            if(pluginServiceWeakReference.get() != null) {
                pluginServiceWeakReference.get().handleMessage(msg);
            }
        }
    }

    private PluginsCreator pluginsCreator;

    private PluginHandler pluginHandler;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        pluginHandler = new PluginHandler(this);

        return new Messenger(pluginHandler).getBinder();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        pluginsCreator = new PluginsCreator();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        pluginsCreator.destroy();
    }

    private static void logi(String message) {
        if(DEBUG) {
            Log.i(TAG, "### " + Thread.currentThread().getId() + " # " + message);
        }
    }

    private void handleMessage(Message msg) {
        logi("PluginService :: handleIncomingMessage()");

        if(msg.what == IPCConstants.MESSAGE_ANDROID) {
            logi("handleIncomingMessage() :: IPCMessageManager.MESSAGE_ANDROID msg.arg1 = " + msg.arg1);

            handleAndroidMessage(msg);
        } else if(msg.what == IPCConstants.MESSAGE_MICROBIT) {
            logi("handleIncomingMessage() :: IPCMessageManager.MESSAGE_MICROBIT msg.arg1 = " + msg.arg1);

            handleMicroBitMessage(msg);
        }
    }

    /**
     * Handler of incoming messages from BLEListener.
     */
    private void handleMicroBitMessage(Message msg) {
        Bundle data = msg.getData();
        CmdArg cmd = new CmdArg(data.getInt(IPCConstants.BUNDLE_DATA), data.getString(IPCConstants.BUNDLE_VALUE));

        logi("handleMicrobitMessage() ## msg.arg1 = " + msg.arg1 + " ## data.getInt=" + data.getInt(IPCConstants
                .BUNDLE_DATA) + " ## data.getString=" + data.getString(IPCConstants.BUNDLE_VALUE));

        AbstractPlugin abstractPlugin = pluginsCreator.createPlugin(msg.arg1, pluginHandler);

        if(abstractPlugin != null) {
            abstractPlugin.handleEntry(cmd);
        }
    }

    private void handleAndroidMessage(Message msg) {
        if(msg.arg1 == EventCategories.IPC_PLUGIN_STOP_PLAYING) {
            AbstractPlugin abstractPlugin = pluginsCreator.createPlugin(EventCategories.SAMSUNG_ALERTS_ID,
                    pluginHandler);

            if(abstractPlugin != null) {
                abstractPlugin.handleEntry(new CmdArg(EventSubCodes.SAMSUNG_ALERT_STOP_PLAYING, null));
            }
        }
    }
}
