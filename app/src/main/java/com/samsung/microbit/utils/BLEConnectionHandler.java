package com.samsung.microbit.utils;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.samsung.microbit.MBApp;
import com.samsung.microbit.R;
import com.samsung.microbit.core.GoogleAnalyticsManager;
import com.samsung.microbit.core.bluetooth.BluetoothUtils;
import com.samsung.microbit.data.constants.Constants;
import com.samsung.microbit.data.constants.EventCategories;
import com.samsung.microbit.data.constants.IPCConstants;
import com.samsung.microbit.data.model.ConnectedDevice;
import com.samsung.microbit.data.model.ui.BaseActivityState;
import com.samsung.microbit.ui.PopUp;

/**
 * Used for make common way of handling connection process.
 */
public class BLEConnectionHandler {
    private BLEConnectionHandler() {
    }

    /**
     * Allows to handle connection between a micro:bit board
     * and a mobile device. It updates connection state UI and
     * changes connection state between STATE_CONNECTED and STATE_DISCONNECTED.
     */
    public static BroadcastReceiver bleConnectionChangedReceiver(final BLEConnectionManager bleConnectionManager) {
        return new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                int error = intent.getIntExtra(IPCConstants.BUNDLE_ERROR_CODE, 0);
                String firmware = intent.getStringExtra(IPCConstants.BUNDLE_MICROBIT_FIRMWARE);
                int getNotification = intent.getIntExtra(IPCConstants.BUNDLE_MICROBIT_REQUESTS, -1);

                bleConnectionManager.preUpdateUi();
                //setConnectedDeviceText();
                if(firmware != null && !firmware.isEmpty()) {
                    BluetoothUtils.updateFirmwareMicrobit(context, firmware);
                    return;
                }

                int mActivityState = bleConnectionManager.getActivityState();

                if(mActivityState == BaseActivityState.STATE_CONNECTING || mActivityState == BaseActivityState
                        .STATE_DISCONNECTING) {

                    if(getNotification == EventCategories.IPC_BLE_NOTIFICATION_INCOMING_CALL ||
                            getNotification == EventCategories.IPC_BLE_NOTIFICATION_INCOMING_SMS) {
                        bleConnectionManager.logi("micro:bit application needs more permissions");
                        bleConnectionManager.addPermissionRequest(getNotification);
                        return;
                    }
                    ConnectedDevice device = BluetoothUtils.getPairedMicrobit(context);
                    if(mActivityState == BaseActivityState.STATE_CONNECTING) {
                        if(error == 0) {
                            GoogleAnalyticsManager.getInstance().sendConnectStats(
                                    BLEConnectionHandler.class.getSimpleName(),
                                    Constants.ConnectionState.SUCCESS, device.mfirmware_version, null);
                            BluetoothUtils.updateConnectionStartTime(context, System.currentTimeMillis());
                            //Check if more permissions were needed and request in the Application
                            if(!bleConnectionManager.arePermissionsGranted()) {
                                bleConnectionManager.setActivityState(BaseActivityState.STATE_IDLE);
                                PopUp.hide();
                                bleConnectionManager.checkTelephonyPermissions();
                                return;
                            }
                        } else {
                            GoogleAnalyticsManager.getInstance().sendConnectStats(
                                    BLEConnectionHandler.class.getSimpleName(),
                                    Constants.ConnectionState.FAIL, null, null);
                        }
                    }
                    if(error == 0 && mActivityState == BaseActivityState.STATE_DISCONNECTING) {
                        long now = System.currentTimeMillis();
                        long connectionTime = (now - device.mlast_connection_time) / 1000; //Time in seconds
                        GoogleAnalyticsManager.getInstance().sendConnectStats(
                                BLEConnectionHandler.class.getSimpleName(),
                                Constants.ConnectionState.DISCONNECT, device.mfirmware_version, Long.toString(connectionTime));
                    }

                    bleConnectionManager.setActivityState(BaseActivityState.STATE_IDLE);
                    PopUp.hide();

                    if(error != 0) {
                        String message = intent.getStringExtra(IPCConstants.BUNDLE_ERROR_MESSAGE);
                        bleConnectionManager.logi("localBroadcastReceiver Error message = " + message);
                        MBApp application = MBApp.getApp();

                        PopUp.show(application.getString(R.string.micro_bit_reset_msg),
                                application.getString(R.string.general_error_title),
                                R.drawable.error_face, R.drawable.red_btn,
                                PopUp.GIFF_ANIMATION_ERROR,
                                PopUp.TYPE_ALERT, null, null);
                    } else {
                        //If All success, change indicator to "not just paired"
                        if(MBApp.getApp().isJustPaired()) {
                            MBApp.getApp().setJustPaired(false);
                        }
                    }
                }
            }
        };
    }

    public interface BLEConnectionManager {
        void setActivityState(int baseActivityState);

        void preUpdateUi();

        int getActivityState();

        void logi(String message);

        void checkTelephonyPermissions();

        void addPermissionRequest(int permission);

        boolean arePermissionsGranted();
    }
}