package com.samsung.microbit.core.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.provider.Settings;
import android.util.Log;

import com.google.gson.Gson;
import com.samsung.microbit.MBApp;
import com.samsung.microbit.data.model.ConnectedDevice;

import java.util.Set;

import static com.samsung.microbit.BuildConfig.DEBUG;

public class BluetoothUtils {
    private static final String TAG = BluetoothUtils.class.getSimpleName();

    public static final String PREFERENCES_KEY = "Microbit_PairedDevices";
    public static final String PREFERENCES_PAIREDDEV_KEY = "PairedDeviceDevice";

    private static ConnectedDevice sConnectedDevice = new ConnectedDevice();

    private static void logi(String message) {
        if(DEBUG) {
            Log.i(TAG, "### " + Thread.currentThread().getId() + " # " + message);
        }
    }

    public static SharedPreferences getPreferences(Context ctx) {

        logi("getPreferences() :: ctx.getApplicationContext() = " + ctx.getApplicationContext());
        return ctx.getApplicationContext().getSharedPreferences(PREFERENCES_KEY, Context.MODE_MULTI_PROCESS);
    }

    public static int getTotalPairedMicroBitsFromSystem() {
        int totalPairedMicroBits = 0;
        BluetoothAdapter mBluetoothAdapter = ((BluetoothManager) MBApp.getApp().getSystemService(Context
                .BLUETOOTH_SERVICE)).getAdapter();
        if(mBluetoothAdapter != null && !mBluetoothAdapter.isEnabled()) {
            Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
            for(BluetoothDevice bt : pairedDevices) {
                if(bt.getName().contains("micro:bit")) {
                    ++totalPairedMicroBits;
                }
            }
        }
        return totalPairedMicroBits;
    }

    public static String parse(final BluetoothGattCharacteristic characteristic) {
        final char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();
        final byte[] data = characteristic.getValue();
        if(data == null)
            return "";
        final int length = data.length;
        if(length == 0)
            return "";

        final char[] out = new char[length * 3 - 1];
        for(int j = 0; j < length; j++) {
            int v = data[j] & 0xFF;
            out[j * 3] = HEX_ARRAY[v >>> 4];
            out[j * 3 + 1] = HEX_ARRAY[v & 0x0F];
            if(j != length - 1)
                out[j * 3 + 2] = '-';
        }
        return new String(out);
    }

    public static boolean inZenMode(Context paramContext) {
        /*
         /**
         * Defines global zen mode.  ZEN_MODE_OFF, ZEN_MODE_IMPORTANT_INTERRUPTIONS,

         public static final String ZEN_MODE = "zen_mode";
         public static final int ZEN_MODE_OFF = 0;
         public static final int ZEN_MODE_IMPORTANT_INTERRUPTIONS = 1;
         public static final int ZEN_MODE_NO_INTERRUPTIONS = 2;
         public static final int ZEN_MODE_ALARMS = 3;
        */
        int zenMode = Settings.Global.getInt(paramContext.getContentResolver(), "zen_mode", 0);
        Log.i("MicroBit", "zen_mode : " + zenMode);
        return (zenMode != 0);
    }

    public static void updateFirmwareMicrobit(Context ctx, String firmware) {
        SharedPreferences pairedDevicePref = ctx.getApplicationContext().getSharedPreferences(PREFERENCES_KEY,
                Context.MODE_MULTI_PROCESS);
        if(pairedDevicePref.contains(PREFERENCES_PAIREDDEV_KEY)) {
            String pairedDeviceString = pairedDevicePref.getString(PREFERENCES_PAIREDDEV_KEY, null);
            Log.v("BluetoothUtils", "Updating the microbit firmware");
            ConnectedDevice deviceInSharedPref = new Gson().fromJson(pairedDeviceString, ConnectedDevice.class);
            deviceInSharedPref.mfirmware_version = firmware;
            setPairedMicroBit(ctx, deviceInSharedPref);
        }
    }

    public static void updateConnectionStartTime(Context ctx, long time) {
        SharedPreferences pairedDevicePref = ctx.getApplicationContext().getSharedPreferences(PREFERENCES_KEY,
                Context.MODE_MULTI_PROCESS);
        if(pairedDevicePref.contains(PREFERENCES_PAIREDDEV_KEY)) {
            String pairedDeviceString = pairedDevicePref.getString(PREFERENCES_PAIREDDEV_KEY, null);
            Log.e("BluetoothUtils", "Updating the microbit firmware");
            ConnectedDevice deviceInSharedPref = new Gson().fromJson(pairedDeviceString, ConnectedDevice.class);
            deviceInSharedPref.mlast_connection_time = time;
            setPairedMicroBit(ctx, deviceInSharedPref);
        }
    }

    public static BluetoothDevice getPairedDeviceMicroBit(Context context) {
        SharedPreferences pairedDevicePref = context.getApplicationContext().getSharedPreferences(PREFERENCES_KEY,
                Context.MODE_MULTI_PROCESS);
        if(pairedDevicePref.contains(PREFERENCES_PAIREDDEV_KEY)) {
            String pairedDeviceString = pairedDevicePref.getString(PREFERENCES_PAIREDDEV_KEY, null);
            Gson gson = new Gson();
            sConnectedDevice = gson.fromJson(pairedDeviceString, ConnectedDevice.class);
            //Check if the microbit is still paired with our mobile
            BluetoothAdapter mBluetoothAdapter = ((BluetoothManager) MBApp.getApp().getSystemService(Context
                    .BLUETOOTH_SERVICE)).getAdapter();
            if(mBluetoothAdapter.isEnabled()) {
                Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
                for(BluetoothDevice bt : pairedDevices) {
                    if(bt.getAddress().equals(sConnectedDevice.mAddress)) {
                        return bt;
                    }
                }
            }
        }
        return null;
    }

    public static ConnectedDevice getPairedMicrobit(Context ctx) {
        SharedPreferences pairedDevicePref = ctx.getApplicationContext().getSharedPreferences(PREFERENCES_KEY,
                Context.MODE_MULTI_PROCESS);

        if(sConnectedDevice == null) {
            sConnectedDevice = new ConnectedDevice();
        }

        if(pairedDevicePref.contains(PREFERENCES_PAIREDDEV_KEY)) {
            boolean pairedMicrobitInSystemList = false;
            String pairedDeviceString = pairedDevicePref.getString(PREFERENCES_PAIREDDEV_KEY, null);
            Gson gson = new Gson();
            sConnectedDevice = gson.fromJson(pairedDeviceString, ConnectedDevice.class);
            //Check if the microbit is still paired with our mobile
            BluetoothAdapter mBluetoothAdapter = ((BluetoothManager) MBApp.getApp().getSystemService(Context
                    .BLUETOOTH_SERVICE)).getAdapter();
            if(mBluetoothAdapter.isEnabled()) {
                Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
                for(BluetoothDevice bt : pairedDevices) {
                    if(bt.getAddress().equals(sConnectedDevice.mAddress)) {
                        pairedMicrobitInSystemList = true;
                        break;
                    }
                }
            } else {
                //Do not change the list until the Bluetooth is back ON again
                pairedMicrobitInSystemList = true;
            }

            if(!pairedMicrobitInSystemList) {
                Log.e("BluetoothUtils", "The last paired microbit is no longer in the system list. Hence removing it");
                //Return a NULL device & update preferences
                sConnectedDevice.mPattern = null;
                sConnectedDevice.mName = null;
                sConnectedDevice.mStatus = false;
                sConnectedDevice.mAddress = null;
                sConnectedDevice.mPairingCode = 0;
                sConnectedDevice.mfirmware_version = null;
                sConnectedDevice.mlast_connection_time = 0;

                setPairedMicroBit(ctx, null);
            }
        } else {
            sConnectedDevice.mPattern = null;
            sConnectedDevice.mName = null;
        }
        return sConnectedDevice;
    }

    public static void setPairedMicroBit(Context ctx, ConnectedDevice newDevice) {
        SharedPreferences pairedDevicePref = ctx.getApplicationContext().getSharedPreferences(PREFERENCES_KEY,
                Context.MODE_MULTI_PROCESS);
        SharedPreferences.Editor editor = pairedDevicePref.edit();
        if(newDevice == null) {
            editor.clear();
        } else {
            Gson gson = new Gson();
            String jsonActiveDevice = gson.toJson(newDevice);
            editor.putString(PREFERENCES_PAIREDDEV_KEY, jsonActiveDevice);
        }

        editor.apply();
    }
}
