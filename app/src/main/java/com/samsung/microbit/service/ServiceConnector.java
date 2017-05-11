package com.samsung.microbit.service;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;

import com.samsung.microbit.core.bluetooth.BluetoothUtils;
import com.samsung.microbit.data.constants.EventCategories;
import com.samsung.microbit.data.constants.IPCConstants;
import com.samsung.microbit.data.model.ConnectedDevice;
import com.samsung.microbit.utils.ServiceUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * Class is used to make connection between {@link IPCService} and other services, will be used in IPC interaction.
 */
public class ServiceConnector {

    private static final int COUNT_SERVICES_FOR_BINDING = 3;

    /**
     * Messenger for sending messages to the service.
     */
    Map<String, Messenger> mServiceMessengers = new HashMap<>(COUNT_SERVICES_FOR_BINDING);
    /**
     * Messenger for receiving messages from the service.
     */
    Messenger mClientMessenger = null;

    /**
     * Handler thread to avoid running on the main thread (UI)
     */
    private final HandlerThread handlerThread;

    /**
     * Handler of incoming messages from service.
     */
    static class IncomingHandler extends Handler {

        public IncomingHandler(HandlerThread thr) {
            super(thr.getLooper());
        }

        private Handler handlingHandler;

        public void setHandlingHandler(Handler handlingHandler) {
            this.handlingHandler = handlingHandler;
        }

        @Override
        public void handleMessage(Message msg) {
            if(handlingHandler != null) {
                handlingHandler.handleMessage(msg);
            }
        }
    }

    /**
     * Flag indicating whether we have called bind on the service.
     */
    boolean mBound;

    /**
     * Context of the activity from which this connector was launched
     */
    private Context mCtx;

    private IncomingHandler handler;

    /**
     * Class for interacting with the main interface of the service.
     */
    private ServiceUtils.IMessengerFinder mConnection = new ServiceUtils.IMessengerFinder() {

        private int countBoundServices = 0;

        public void onServiceConnected(ComponentName className, IBinder service) {
            // This is called when the connection with the service has been
            // established, giving us the object we can use to
            // interact with the service. We are communicating with the
            // service using a Messenger, so here we get a client-side
            // representation of that from the raw IBinder object.
            Messenger mServiceMessenger = new Messenger(service);

            mServiceMessengers.put(className.getClassName(), mServiceMessenger);

            if(++countBoundServices == COUNT_SERVICES_FOR_BINDING) {
                ConnectedDevice connectedDevice = BluetoothUtils.getPairedMicrobit(mCtx);

                if(connectedDevice.mStatus) {
                    Intent intent = new Intent(mCtx, IPCService.class);
                    intent.putExtra(IPCConstants.INTENT_TYPE, EventCategories.IPC_BLE_CONNECT);
                    mCtx.startService(intent);
                }
            }

            mBound = true;
        }

        public void onServiceDisconnected(ComponentName className) {
            // This is called when the connection with the service has been
            // unexpectedly disconnected -- that is, its process crashed.
            mServiceMessengers.remove(className.getClassName());
            mBound = false;
        }

        @Override
        public Messenger getMessengerForService(String serviceName) {
            return mServiceMessengers.get(serviceName);
        }
    };

    public ServiceConnector(Context ctx) {
        mCtx = ctx;
        handlerThread = new HandlerThread("IPChandlerThread");
        handlerThread.start();
        handler = new IncomingHandler(handlerThread);
        mClientMessenger = new Messenger(handler);
    }

    public void setClientHandler(Handler clientHandler) {
        handler.setHandlingHandler(clientHandler);
    }

    /**
     * Method used for binding with the service
     */
    public void bindServices() {
        /*
         * Note that this is an implicit Intent that must be defined in the
         * Android Manifest.
         */
        Intent i = new Intent(mCtx, IPCService.class);

        Context context = mCtx.getApplicationContext();
        context.bindService(i, mConnection, Context.BIND_AUTO_CREATE);

        i = new Intent(mCtx, BLEService.class);
        context.bindService(i, mConnection, Context.BIND_AUTO_CREATE);
        i = new Intent(mCtx, PluginService.class);
        context.bindService(i, mConnection, Context.BIND_AUTO_CREATE);
    }

    public void unbindServices() {
        if(mBound) {
            mCtx.getApplicationContext().unbindService(mConnection);
            mBound = false;

            handlerThread.quitSafely();
        }
    }

    public ServiceUtils.IMessengerFinder getConnection() {
        return mConnection;
    }
}