package com.samsung.microbit.service;

import android.app.NotificationManager;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.Toast;

import com.samsung.microbit.MBApp;
import com.samsung.microbit.R;
import com.samsung.microbit.core.bluetooth.BLEManager;
import com.samsung.microbit.core.bluetooth.BluetoothUtils;
import com.samsung.microbit.core.bluetooth.CharacteristicChangeListener;
import com.samsung.microbit.core.bluetooth.UnexpectedConnectionEventListener;
import com.samsung.microbit.data.constants.CharacteristicUUIDs;
import com.samsung.microbit.data.constants.Constants;
import com.samsung.microbit.data.constants.EventCategories;
import com.samsung.microbit.data.constants.GattFormats;
import com.samsung.microbit.data.constants.GattServiceUUIDs;
import com.samsung.microbit.data.constants.IPCConstants;
import com.samsung.microbit.data.constants.RegistrationIds;
import com.samsung.microbit.data.constants.ServiceIds;
import com.samsung.microbit.data.constants.UUIDs;
import com.samsung.microbit.data.model.CmdArg;
import com.samsung.microbit.data.model.ConnectedDevice;
import com.samsung.microbit.data.model.NameValuePair;
import com.samsung.microbit.utils.ServiceUtils;

import java.lang.ref.WeakReference;
import java.util.List;
import java.util.UUID;

import no.nordicsemi.android.error.GattError;

import static com.samsung.microbit.BuildConfig.DEBUG;

public class BLEService extends Service {
    private static final String TAG = BLEService.class.getSimpleName();

    private static final int ERROR_NONE = 0;
    private static final int ERROR_TIME_OUT = 10;
    private static final int ERROR_UNKNOWN_1 = 99;
    private static final int ERROR_UNKNOWN_2 = 1;
    private static final int ERROR_UNKNOWN_3 = 2;

    public static final int SIMULATE = 10;

    public static final String GATT_FORCE_CLOSED = "com.microbit.gatt_force_closed";

    private static final class BLEHandler extends Handler {
        private WeakReference<BLEService> bleServiceWeakReference;

        private BLEHandler(BLEService bleService) {
            super();
            bleServiceWeakReference = new WeakReference<>(bleService);
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if(bleServiceWeakReference.get() != null) {
                bleServiceWeakReference.get().handleMessage(msg);
            }
        }
    }

    public static final boolean AUTO_RECONNECT = false;

    private BLEManager bleManager;

    private BluetoothManager bluetoothManager;
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothDevice bluetoothDevice;

    private String deviceAddress;

    private int actualError = ERROR_NONE;

    private Messenger inputMessenger;

    private BLEHandler bleHandler;

    private ServiceConnection connection;

    @Override
    public void onCreate() {
        super.onCreate();

        connection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {

            }

            @Override
            public void onServiceDisconnected(ComponentName name) {

            }
        };

        //TODO This is HACK for android not allow to kill IPCService
        bindService(new Intent(this, IPCService.class), connection, BIND_IMPORTANT);
    }

    @Override
    public void onDestroy() {
        if(connection != null) {
            unbindService(connection);
        }
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        bleHandler = new BLEHandler(this);

        return new Messenger(bleHandler).getBinder();
    }

    @Override
    public boolean onUnbind(Intent intent) {
        if(bleHandler != null) {
            Message disconnectMessage = Message.obtain(null, IPCConstants.MESSAGE_ANDROID);
            disconnectMessage.arg1 = EventCategories.IPC_BLE_DISCONNECT;
            bleHandler.sendMessage(disconnectMessage);
        }
        return super.onUnbind(intent);
    }

    private void handleMessage(Message msg) {
        inputMessenger = msg.replyTo;
        logi("handleIncomingMessage()");
        Bundle bundle = msg.getData();
        if(msg.what == IPCConstants.MESSAGE_ANDROID) {
            logi("IPCMessageManager.MESSAGE_ANDROID msg.arg1 = " + msg.arg1);
            if(msg.arg1 == SIMULATE) {
                try {
                    Thread.sleep(10000);
                } catch(InterruptedException e) {
                    Log.e(TAG, e.toString());
                }
                sendMessage(EventCategories.SAMSUNG_REMOTE_CONTROL_ID, 10);
                return;
            }

            switch(msg.arg1) {
                case EventCategories.IPC_BLE_CONNECT:
                    int justPaired = msg.arg2;
                    if(justPaired == IPCConstants.JUST_PAIRED) {
                        Log.e(TAG, "just paired delay");
                        try {
                            Thread.sleep(Constants.JUST_PAIRED_DELAY_ON_CONNECTION);
                        } catch(InterruptedException e) {
                            Log.e(TAG, e.toString());
                        }
                    } else {
                        Log.e(TAG, "paired earlier");
                    }
                    setupBLE();
                    break;

                case EventCategories.IPC_BLE_DISCONNECT:
                    initBLEManager();
                    if(reset()) {
                        setNotification(false, ERROR_NONE);
                    }

                    break;

                case EventCategories.IPC_BLE_RECONNECT:
                    if(reset()) {
                        setupBLE();
                    }

                    break;

                default:
            }
        } else if(msg.what == IPCConstants.MESSAGE_MICROBIT) {
            logi("IPCMessageManager.MESSAGE_MICROBIT msg.arg1 = " + msg.arg1);
            switch(msg.arg1) {
                case EventCategories.IPC_WRITE_CHARACTERISTIC:
                    String service = (String) bundle.getSerializable(IPCConstants.BUNDLE_SERVICE_GUID);
                    String characteristic = (String) bundle.getSerializable(IPCConstants.BUNDLE_CHARACTERISTIC_GUID);
                    int value = (int) bundle.getSerializable(IPCConstants.BUNDLE_CHARACTERISTIC_VALUE);
                    int type = (int) bundle.getSerializable(IPCConstants.BUNDLE_CHARACTERISTIC_TYPE);
                    writeCharacteristic(service, characteristic, value, type);
                    break;

                default:
            }
        }
    }

    /**
     * Simplified method to log informational messages.
     *
     * @param message Message to log.
     */
    private static void logi(String message) {
        if(DEBUG) {
            Log.i(TAG, "### " + Thread.currentThread().getId() + " # " + message);
        }
    }

    /**
     * Disconnects all devices and resets bluetooth manager.
     *
     * @return True, if successful.
     */
    private boolean reset() {
        boolean rc = false;
        if(bleManager != null) {
            disconnectAll();
            rc = bleManager.reset();
            if(rc) {
                bleManager = null;
            }
        }

        return rc;
    }

    private void disconnectAll() {
        logi("disconnectAll()");
        registerNotifications(false);
    }

    private boolean registerNotifications(boolean enable) {
        logi("registerNotifications() : " + enable);

        //Read micro:bit firmware version
        BluetoothGattService deviceInfoService = getService(GattServiceUUIDs.DEVICE_INFORMATION_SERVICE);
        if(deviceInfoService != null) {
            BluetoothGattCharacteristic firmwareCharacteristic = deviceInfoService.getCharacteristic
                    (CharacteristicUUIDs.FIRMWARE_REVISION_UUID);
            if(firmwareCharacteristic != null) {
                String firmware = "";
                BluetoothGattCharacteristic characteristic = readCharacteristic(firmwareCharacteristic);
                if(characteristic != null && characteristic.getValue() != null && characteristic.getValue().length != 0) {
                    firmware = firmwareCharacteristic.getStringValue(0);
                }
                sendMicrobitFirmware(firmware);
                logi("Micro:bit firmware version String = " + firmware);
            }
        } else {
            Log.e(TAG, "Not found DeviceInformationService");
        }

        BluetoothGattService eventService = getService(GattServiceUUIDs.EVENT_SERVICE);
        if(eventService == null) {
            Log.e(TAG, "Not found EventService");
            logi("registerNotifications() :: not found service : Constants.EVENT_SERVICE");
            return false;
        }

        logi("Constants.EVENT_SERVICE   = " + GattServiceUUIDs.EVENT_SERVICE.toString());
        logi("Constants.ES_MICROBIT_REQUIREMENTS   = " + CharacteristicUUIDs.ES_MICROBIT_REQUIREMENTS.toString());
        logi("Constants.ES_CLIENT_EVENT   = " + CharacteristicUUIDs.ES_CLIENT_EVENT.toString());
        logi("Constants.ES_MICROBIT_EVENT   = " + CharacteristicUUIDs.ES_MICROBIT_EVENT.toString());
        logi("Constants.ES_CLIENT_REQUIREMENTS   = " + CharacteristicUUIDs.ES_CLIENT_REQUIREMENTS.toString());
        if(!registerMicrobitRequirements(eventService, enable)) {
            if(DEBUG) {
                logi("***************** Cannot Register Microbit Requirements.. Will continue ************** ");
            }
        }

        register_AppRequirement(eventService, enable);

        if(!registerMicroBitEvents(eventService, enable)) {
            logi("Failed to registerMicroBitEvents");
            return false;
        }
        logi("registerNotifications() : done");
        return true;
    }

    private BluetoothGattService getService(UUID uuid) {
        if(bleManager != null) {
            return bleManager.getService(uuid);
        }

        return null;
    }

    /**
     * Reads the requested characteristic from the associated remote device.
     *
     * @param characteristic Characteristic to read from the remote device.
     * @return Operation result code.
     */
    private BluetoothGattCharacteristic readCharacteristic(BluetoothGattCharacteristic characteristic) {
        if(bleManager != null) {
            int rc = bleManager.readCharacteristic(characteristic);
            rc = interpretCode(rc);
            if(rc == ERROR_NONE) {
                return bleManager.getLastCharacteristic();
            }
        }

        return null;
    }

    /**
     * Interprets code result number and returns new result number.
     *
     * @param rc Result code number.
     * @return New result code number.
     */
    private int interpretCode(int rc) {
        if(rc > 0) {
            if((rc & BLEManager.BLE_ERROR_FAIL) != 0) {
                if((rc & BLEManager.BLE_ERROR_TIMEOUT) != 0) {
                    rc = ERROR_TIME_OUT;
                } else {
                    rc = ERROR_UNKNOWN_1;
                }
            } else {
                rc &= 0x0000ffff;
                if(rc == BLEManager.BLE_DISCONNECTED) {
                    rc = ERROR_UNKNOWN_2;
                } else {
                    rc = ERROR_NONE;
                }
            }
        }

        return rc;
    }

    private void sendMicrobitFirmware(String firmware) {
        if(inputMessenger == null) {
            Log.e(TAG, "wrong inputMessenger");
            return;
        }

        NameValuePair[] args = new NameValuePair[2];
        args[0] = new NameValuePair(IPCConstants.BUNDLE_ERROR_CODE, 0);
        args[1] = new NameValuePair(IPCConstants.BUNDLE_MICROBIT_FIRMWARE, firmware);

        try {
            inputMessenger.send(ServiceUtils.composeMessage(IPCConstants.MESSAGE_ANDROID, EventCategories
                    .IPC_BLE_NOTIFICATION_CHARACTERISTIC_CHANGED, ServiceIds.SERVICE_NONE, null, args));
        } catch(RemoteException e) {
            Log.e(TAG, e.toString());
        }
    }

    /**
     * Register to know about the micro:bit requirements. What events does the micro:bit need from us
     * read repeatedly from (3) to find out the events that the micro:bit is interested in receiving.
     * e.g. if a kids app registers to receive events <10,3><15,2> then the first read will
     * give you <10,3> the second <15,2>, the third will give you a zero length value.
     * You can send events to the micro:bit that haven't been asked for, but as no-one will
     * be listening, they will be silently dropped.
     *
     * @param eventService Bluetooth GATT service.
     * @param enable       Enable or disable.
     * @return True, if successful.
     */
    private boolean registerMicrobitRequirements(BluetoothGattService eventService, boolean enable) {
        BluetoothGattCharacteristic microbit_requirements = eventService.getCharacteristic(CharacteristicUUIDs
                .ES_MICROBIT_REQUIREMENTS);
        if(microbit_requirements == null) {
            logi("register_eventsFromMicrobit() :: ES_MICROBIT_REQUIREMENTS Not found");
            return false;
        }

        BluetoothGattDescriptor microbit_requirementsDescriptor = microbit_requirements.getDescriptor(UUIDs
                .CLIENT_DESCRIPTOR);
        if(microbit_requirementsDescriptor == null) {
            logi("register_eventsFromMicrobit() :: CLIENT_DESCRIPTOR Not found");
            return false;
        }

        BluetoothGattCharacteristic characteristic = readCharacteristic(microbit_requirements);
        while(characteristic != null && characteristic.getValue() != null && characteristic.getValue().length != 0) {
            String service = BluetoothUtils.parse(characteristic);
            logi("microbit interested in  = " + service);
            if(service.equalsIgnoreCase("4F-04-07-00")) //Incoming Call service
            {
                sendMicroBitNeedsCallNotification();
            }
            if(service.equalsIgnoreCase("4F-04-08-00")) //Incoming SMS service
            {
                sendMicroBitNeedsSmsNotification();
            }
            characteristic = readCharacteristic(microbit_requirements);
        }

        registerForSignalStrength(enable);
        registerForDeviceInfo(enable);

        logi("registerMicrobitRequirements() :: found Constants.ES_MICROBIT_REQUIREMENTS ");
        enableCharacteristicNotification(microbit_requirements, microbit_requirementsDescriptor, enable);
        return true;
    }

    private void sendMicroBitNeedsCallNotification() {
        if(inputMessenger == null) {
            Log.e(TAG, "wrong inputMessenger");
            return;
        }

        NameValuePair[] args = new NameValuePair[2];
        args[0] = new NameValuePair(IPCConstants.BUNDLE_ERROR_CODE, 0);
        args[1] = new NameValuePair(IPCConstants.BUNDLE_MICROBIT_REQUESTS, EventCategories
                .IPC_BLE_NOTIFICATION_INCOMING_CALL);

        try {
            inputMessenger.send(ServiceUtils.composeMessage(IPCConstants.MESSAGE_ANDROID, EventCategories
                    .IPC_BLE_NOTIFICATION_CHARACTERISTIC_CHANGED, ServiceIds.SERVICE_NONE, null, args));
        } catch(RemoteException e) {
            Log.e(TAG, e.toString());
        }
    }

    private void sendMicroBitNeedsSmsNotification() {
        if(inputMessenger == null) {
            Log.e(TAG, "wrong inputMessenger");
            return;
        }


        NameValuePair[] args = new NameValuePair[2];
        args[0] = new NameValuePair(IPCConstants.BUNDLE_ERROR_CODE, 0);
        args[1] = new NameValuePair(IPCConstants.BUNDLE_MICROBIT_REQUESTS, EventCategories
                .IPC_BLE_NOTIFICATION_INCOMING_SMS);

        try {
            inputMessenger.send(ServiceUtils.composeMessage(IPCConstants.MESSAGE_ANDROID, EventCategories
                    .IPC_BLE_NOTIFICATION_CHARACTERISTIC_CHANGED, ServiceIds.SERVICE_NONE, null, args));
        } catch(RemoteException e) {
            Log.e(TAG, e.toString());
        }
    }

    /**
     * Registers or unregisters listener for signal strength.
     *
     * @param register Register or unregister.
     */
    private void registerForSignalStrength(boolean register) {
        if(inputMessenger == null) {
            Log.e(TAG, "wrong inputMessenger");
            return;
        }

        CmdArg cmd = register ? new CmdArg(RegistrationIds.REG_SIGNALSTRENGTH, "On") : new CmdArg(RegistrationIds
                .REG_SIGNALSTRENGTH, "Off");

        Message message = ServiceUtils.composeMessage(IPCConstants.MESSAGE_MICROBIT, EventCategories
                .SAMSUNG_SIGNAL_STRENGTH_ID, ServiceIds.SERVICE_PLUGIN, cmd, null);

        if(message != null) {
            try {
                inputMessenger.send(message);
            } catch(RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Registers or unregisters listeners for a device information, such as device orientation,
     * device gesture, device battery strength and etc.
     *
     * @param register Register or unregister.
     */
    private void registerForDeviceInfo(boolean register) {
        if(inputMessenger == null) {
            Log.e(TAG, "wrong inputMessenger");
            return;
        }

        logi("registerForDeviceInfo() -- " + register);

        //Device Orientation
        CmdArg cmd = register ? new CmdArg(RegistrationIds.REG_DEVICEORIENTATION, "On") : new CmdArg(RegistrationIds
                .REG_DEVICEORIENTATION, "Off");
        Message message = ServiceUtils.composeMessage(IPCConstants.MESSAGE_MICROBIT, EventCategories
                .SAMSUNG_DEVICE_INFO_ID, ServiceIds.SERVICE_PLUGIN, cmd, null);
        if(message != null) {
            try {
                inputMessenger.send(message);
            } catch(RemoteException e) {
                e.printStackTrace();
            }
        }

        //Device Gesture
        cmd = register ? new CmdArg(RegistrationIds.REG_DEVICEGESTURE, "On") : new CmdArg(RegistrationIds.REG_DEVICEGESTURE,
                "Off");
        message = ServiceUtils.composeMessage(IPCConstants.MESSAGE_MICROBIT, EventCategories
                .SAMSUNG_DEVICE_INFO_ID, ServiceIds.SERVICE_PLUGIN, cmd, null);
        if(message != null) {
            try {
                inputMessenger.send(message);
            } catch(RemoteException e) {
                e.printStackTrace();
            }
        }

        //Device Battery Strength
        cmd = register ? new CmdArg(RegistrationIds.REG_BATTERYSTRENGTH, "On") : new CmdArg(RegistrationIds.REG_BATTERYSTRENGTH,
                "Off");
        message = ServiceUtils.composeMessage(IPCConstants.MESSAGE_MICROBIT, EventCategories
                .SAMSUNG_DEVICE_INFO_ID, ServiceIds.SERVICE_PLUGIN, cmd, null);
        if(message != null) {
            try {
                inputMessenger.send(message);
            } catch(RemoteException e) {
                e.printStackTrace();
            }
        }

        //Device Temperature
        cmd = register ? new CmdArg(RegistrationIds.REG_TEMPERATURE, "On") : new CmdArg(RegistrationIds.REG_TEMPERATURE, "Off");
        message = ServiceUtils.composeMessage(IPCConstants.MESSAGE_MICROBIT, EventCategories
                .SAMSUNG_DEVICE_INFO_ID, ServiceIds.SERVICE_PLUGIN, cmd, null);
        if(message != null) {
            try {
                inputMessenger.send(message);
            } catch(RemoteException e) {
                e.printStackTrace();
            }
        }

        //Register Telephony
        cmd = register ? new CmdArg(RegistrationIds.REG_TELEPHONY, "On") : new CmdArg(RegistrationIds.REG_TELEPHONY, "Off");
        message = ServiceUtils.composeMessage(IPCConstants.MESSAGE_MICROBIT, EventCategories
                .SAMSUNG_TELEPHONY_ID, ServiceIds.SERVICE_PLUGIN, cmd, null);
        if(message != null) {
            try {
                inputMessenger.send(message);
            } catch(RemoteException e) {
                e.printStackTrace();
            }
        }

        //Register Messaging
        cmd = register ? new CmdArg(RegistrationIds.REG_MESSAGING, "On") : new CmdArg(RegistrationIds.REG_MESSAGING, "Off");
        message = ServiceUtils.composeMessage(IPCConstants.MESSAGE_MICROBIT, EventCategories
                .SAMSUNG_TELEPHONY_ID, ServiceIds.SERVICE_PLUGIN, cmd, null);
        if(message != null) {
            try {
                inputMessenger.send(message);
            } catch(RemoteException e) {
                e.printStackTrace();
            }
        }

        //Register Display
        cmd = register ? new CmdArg(RegistrationIds.REG_DISPLAY, "On") : new CmdArg(RegistrationIds.REG_DISPLAY, "Off");
        message = ServiceUtils.composeMessage(IPCConstants.MESSAGE_MICROBIT, EventCategories
                .SAMSUNG_TELEPHONY_ID, ServiceIds.SERVICE_PLUGIN, cmd, null);
        if(message != null) {
            try {
                inputMessenger.send(message);
            } catch(RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Enable or disable notifications/indications for a given characteristic.
     *
     * @param characteristic The characteristic for which to enable notifications.
     * @param descriptor     Bluetooth GATT descriptor.
     * @param enable         Enable or disable notification.
     * @return Result code number.
     */
    private int enableCharacteristicNotification(BluetoothGattCharacteristic characteristic, BluetoothGattDescriptor
            descriptor, boolean enable) {
        int rc = ERROR_UNKNOWN_1;

        if(bleManager != null) {
            rc = bleManager.enableCharacteristicNotification(characteristic, descriptor, enable);
        }

        return rc;
    }

    /**
     * write repeatedly to (4) to register for the events your app wants to see from the micro:bit.
     * e.g. write <1,1> to register for a 'DOWN' event on ButtonA.
     * Any events matching this will then start to be delivered via the MicroBit Event characteristic.
     *
     * @param eventService Bluetooth GATT service.
     * @param enable       Enable or disable.
     */
    private void register_AppRequirement(BluetoothGattService eventService, boolean enable) {
        if(!enable) {
            return;
        }

        BluetoothGattCharacteristic app_requirements = eventService.getCharacteristic(CharacteristicUUIDs.ES_CLIENT_REQUIREMENTS);
        if(app_requirements != null) {
            logi("register_AppRequirement() :: found Constants.ES_CLIENT_REQUIREMENTS ");
            /*
            Registering for everything at the moment
            <1,0> which means give me all the events from ButtonA.
            <2,0> which means give me all the events from ButtonB.
            <0,0> which means give me all the events from everything.
            writeCharacteristic(Constants.EVENT_SERVICE.toString(), Constants.ES_CLIENT_REQUIREMENTS.toString(), 0, BluetoothGattCharacteristic.FORMAT_UINT32);
            */
            writeCharacteristic(GattServiceUUIDs.EVENT_SERVICE.toString(), CharacteristicUUIDs.ES_CLIENT_REQUIREMENTS.toString(),
                    EventCategories.SAMSUNG_REMOTE_CONTROL_ID, GattFormats.FORMAT_UINT32);
            writeCharacteristic(GattServiceUUIDs.EVENT_SERVICE.toString(), CharacteristicUUIDs.ES_CLIENT_REQUIREMENTS.toString(),
                    EventCategories.SAMSUNG_CAMERA_ID, GattFormats.FORMAT_UINT32);
            writeCharacteristic(GattServiceUUIDs.EVENT_SERVICE.toString(), CharacteristicUUIDs.ES_CLIENT_REQUIREMENTS.toString(),
                    EventCategories.SAMSUNG_ALERTS_ID, GattFormats.FORMAT_UINT32);
            writeCharacteristic(GattServiceUUIDs.EVENT_SERVICE.toString(), CharacteristicUUIDs.ES_CLIENT_REQUIREMENTS.toString(),
                    EventCategories.SAMSUNG_SIGNAL_STRENGTH_ID, GattFormats.FORMAT_UINT32);
            writeCharacteristic(GattServiceUUIDs.EVENT_SERVICE.toString(), CharacteristicUUIDs.ES_CLIENT_REQUIREMENTS.toString(),
                    EventCategories.SAMSUNG_DEVICE_INFO_ID, GattFormats.FORMAT_UINT32);
            //writeCharacteristic(GattServiceUUIDs.EVENT_SERVICE.toString(), CharacteristicUUIDs
            //        .ES_CLIENT_REQUIREMENTS.toString(), EventCategories.SAMSUNG_TELEPHONY_ID,
            //        GattFormats.FORMAT_UINT32);
        }
    }

    private void writeCharacteristic(String serviceGuid, String characteristic, int value, int type) {
        if(!isConnected()) {
            logi("writeCharacteristic() :: Not connected. Returning");
            return;
        }

        BluetoothGattService s = getService(UUID.fromString(serviceGuid));
        if(s == null) {
            logi("writeCharacteristic() :: Service not found");
            return;
        }

        BluetoothGattCharacteristic c = s.getCharacteristic(UUID.fromString(characteristic));
        if(c == null) {
            logi("writeCharacteristic() :: characteristic not found");
            return;
        }

        c.setValue(value, type, 0);
        int ret = writeCharacteristic(c);
        logi("writeCharacteristic() :: returns - " + ret);
    }

    private boolean isConnected() {
        return bleManager != null && bleManager.isConnected();
    }

    /**
     * Writes a given characteristic and its values to the associated remote device.
     *
     * @param characteristic Characteristic to write on the remote device.
     * @return Operation result code.
     */
    private int writeCharacteristic(BluetoothGattCharacteristic characteristic) {
        int rc = ERROR_UNKNOWN_1;

        if(bleManager != null) {
            rc = bleManager.writeCharacteristic(characteristic);
            rc = interpretCode(rc);

            logi("Data written to " + characteristic.getUuid() + " value : (0x)" + BluetoothUtils.parse
                    (characteristic) + " Return Value = 0x" + Integer.toHexString(rc));
        }
        return rc;
    }

    /**
     * Enables or disables micro:bit event by given event and enable/disable flag.
     *
     * @param eventService Bluetooth GATT service to be registered.
     * @param enable       Enable or disable.
     * @return True, if successful.
     */
    private boolean registerMicroBitEvents(BluetoothGattService eventService, boolean enable) {
        // Read (or register for notify) on (1) to receive events generated by the micro:bit.
        BluetoothGattCharacteristic microbit_requirements = eventService.getCharacteristic(CharacteristicUUIDs
                .ES_MICROBIT_EVENT);
        if(microbit_requirements == null) {
            logi("register_eventsFromMicrobit() :: ES_MICROBIT_EVENT Not found");
            return false;
        }
        BluetoothGattDescriptor microbit_requirementsDescriptor = microbit_requirements.getDescriptor(UUIDs
                .CLIENT_DESCRIPTOR);
        if(microbit_requirementsDescriptor == null) {
            logi("register_eventsFromMicrobit() :: CLIENT_DESCRIPTOR Not found");
            return false;
        }

        enableCharacteristicNotification(microbit_requirements, microbit_requirementsDescriptor, enable);
        return true;
    }

    /**
     * Setups bluetooth low energy service.
     */
    private void setupBLE() {
        initBLEManager();

        startupConnection();
    }

    private void initBLEManager() {
        if(bleManager != null) {
            return;
        }

        logi("setupBLE()");

        this.deviceAddress = searchDeviceAddress();
        logi("setupBLE() :: deviceAddress = " + deviceAddress);

        if(deviceAddress == null) {
            setNotification(false, ERROR_UNKNOWN_2);
            return;
        }

        bluetoothDevice = null;

        if(!initialize()) {
            setNotification(false, ERROR_UNKNOWN_2);
            return;
        }

        bleManager = new BLEManager(getApplicationContext(), bluetoothDevice,
                new CharacteristicChangeListener() {
                    @Override
                    public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic
                            characteristic) {
                        logi("setupBLE().CharacteristicChangeListener.onCharacteristicChanged()");

                        if(bleManager != null) {
                            handleCharacteristicChanged(gatt, characteristic);
                        }
                    }
                },
                new UnexpectedConnectionEventListener() {
                    @Override
                    public void handleConnectionEvent(int event, boolean gattForceClosed) {
                        logi("setupBLE().CharacteristicChangeListener.handleUnexpectedConnectionEvent()"
                                + event);

                        if(bleManager != null) {
                            handleUnexpectedConnectionEvent(event, gattForceClosed);
                        }
                    }
                });

    }

    private String searchDeviceAddress() {
        logi("getDeviceAddress()");

        ConnectedDevice currentDevice = BluetoothUtils.getPairedMicrobit(this);
        String deviceAddress = currentDevice.mAddress;
        if(deviceAddress == null) {
            setNotification(false, ERROR_UNKNOWN_3);
        }

        return deviceAddress;
    }

    private void setNotification(boolean isConnected, int errorCode) {
        int actual_Error = actualError;

        logi("setNotification() :: isConnected = " + isConnected);
        logi("setNotification() :: errorCode = " + errorCode);
        logi("setNotification() :: actual_Error = " + actual_Error);


        NotificationManager notifyMgr = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        String notificationString = null;
        boolean onGoingNotification = false;

        NameValuePair[] args = new NameValuePair[3];

        args[0] = new NameValuePair(IPCConstants.BUNDLE_ERROR_CODE, errorCode);
        args[1] = new NameValuePair(IPCConstants.BUNDLE_DEVICE_ADDRESS, deviceAddress);
        args[2] = new NameValuePair(IPCConstants.BUNDLE_ERROR_MESSAGE, GattError.parse(actual_Error));

        if(!isConnected) {
            logi("setNotification() :: !isConnected");

            if(bluetoothAdapter != null) {
                if(!bluetoothAdapter.isEnabled()) {

                    logi("setNotification() :: !bluetoothAdapter.isEnabled()");
                    reset();
                    //bleManager = null;
                    bluetoothDevice = null;
                } else {
                    //bluetoothAdapter.disable();
                }
            }
            notificationString = getString(R.string.tray_notification_failure);
            onGoingNotification = false;

            if(inputMessenger != null) {
                Message message = ServiceUtils.composeMessage(IPCConstants.MESSAGE_ANDROID, EventCategories
                        .IPC_BLE_NOTIFICATION_GATT_DISCONNECTED, ServiceIds.SERVICE_NONE, null, args);
                if(message != null) {
                    try {
                        inputMessenger.send(message);
                    } catch(RemoteException e) {
                        Log.e(TAG, e.toString());
                    }
                }
            }
        } else {
            notificationString = getString(R.string.tray_notification_sucsess);
            onGoingNotification = true;

            if(inputMessenger != null) {
                Message message = ServiceUtils.composeMessage(IPCConstants.MESSAGE_ANDROID, EventCategories
                        .IPC_BLE_NOTIFICATION_GATT_CONNECTED, ServiceIds.SERVICE_NONE, null, args);
                if(message != null) {
                    try {
                        inputMessenger.send(message);
                    } catch(RemoteException e) {
                        Log.e(TAG, e.toString());
                    }
                }
            }
        }

        //TODO use notificationString, notifyMgr, and onGoingNotification
    }

    private void handleCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
        String UUID = characteristic.getUuid().toString();

        Integer integerValue = characteristic.getIntValue(GattFormats.FORMAT_UINT32, 0);

        if(integerValue == null) {
            return;
        }
        int value = integerValue;
        int eventSrc = value & 0x0ffff;
        if(eventSrc < 1001) {
            return;
        }
        logi("Characteristic UUID = " + UUID);
        logi("Characteristic Value = " + value);
        logi("eventSrc = " + eventSrc);

        int event = (value >> 16) & 0x0ffff;
        logi("event = " + event);
        sendMessage(eventSrc, event);
    }

    private void sendMessage(int eventSrc, int event) {
        if(inputMessenger == null) {
            Log.e(TAG, "wrong inputMessenger");
            return;
        }

        logi("Sending eventSrc " + eventSrc + "  event=" + event);
        int msgService;
        CmdArg cmd;
        switch(eventSrc) {
            case EventCategories.SAMSUNG_REMOTE_CONTROL_ID:
            case EventCategories.SAMSUNG_ALERTS_ID:
            case EventCategories.SAMSUNG_AUDIO_RECORDER_ID:
            case EventCategories.SAMSUNG_CAMERA_ID:
                msgService = eventSrc;
                cmd = new CmdArg(event, "1000");
                break;

            default:
                Log.e(TAG, "unknown category: " + eventSrc);
                return;
        }

        Message message = ServiceUtils.composeMessage(IPCConstants.MESSAGE_MICROBIT,
                msgService, ServiceIds.SERVICE_PLUGIN, cmd, null);
        if(message != null) {
            try {
                inputMessenger.send(message);
            } catch(RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    private void handleUnexpectedConnectionEvent(int event, boolean gattForceClosed) {
        logi("handleUnexpectedConnectionEvent() :: event = " + event);

        //TODO make something when unexpected disconnect happened
        /*if(gattForceClosed) {
            Context appContext = getApplicationContext();

            BluetoothDevice pairedDevice = BluetoothUtils.getPairedDeviceMicrobit(appContext);

            if(pairedDevice != null) {
                removeBond(pairedDevice);
                BluetoothUtils.setPairedMicroBit(appContext, null);
                LocalBroadcastManager.getInstance(appContext).sendBroadcast(new Intent(GATT_FORCE_CLOSED));
            }
            return;
        }*/

        if((event & BLEManager.BLE_CONNECTED) != 0) {
            logi("handleUnexpectedConnectionEvent() :: BLE_CONNECTED");
            discoverServices();
            registerNotifications(true);
            setNotification(true, ERROR_NONE);
        } else if(event == BLEManager.BLE_DISCONNECTED) {
            logi("handleUnexpectedConnectionEvent() :: BLE_DISCONNECTED");
            setNotification(false, ERROR_NONE);
        }
    }

    /**
     * Provides asynchronous operation to discover for services. If the discovery was
     * successful, the remote services can be accessed through {@link #getServices()} method.
     *
     * @return Result of discovering.
     */
    private int discoverServices() {
        int rc = ERROR_UNKNOWN_1;

        if(bleManager != null) {
            if(DEBUG) {
                logi("discoverServices() :: bleManager != null");
            }

            rc = bleManager.discoverServices();
            rc = interpretCode(rc, BLEManager.BLE_SERVICES_DISCOVERED);
        }

        return rc;
    }

    /**
     * Interprets a result code number comparing with expected code number.
     *
     * @param rc       Result code number.
     * @param goodCode Expected code number.
     * @return New result code.
     */
    private int interpretCode(int rc, int goodCode) {
        if(rc > 0) {
            if((rc & BLEManager.BLE_ERROR_FAIL) != 0) {
                actualError = bleManager.getExtendedError();
                if((rc & BLEManager.BLE_ERROR_TIMEOUT) != 0) {
                    rc = ERROR_TIME_OUT;
                } else {
                    rc = ERROR_UNKNOWN_1;
                }
            } else {
                actualError = 0;
                rc &= 0x0ffff;
                if((rc & goodCode) != 0) {
                    rc = ERROR_NONE;
                } else {
                    rc = ERROR_UNKNOWN_2;
                }
            }
        }

        return rc;
    }

    /**
     * Initializes bluetooth manager.
     *
     * @return True, if successful.
     */
    private boolean initialize() {
        logi("initialize() :: remoteDevice = " + deviceAddress);

        boolean rc = true;

        if(bluetoothManager == null) {
            bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            rc = bluetoothManager != null;
        }

        if(rc && (bluetoothAdapter == null)) {
            bluetoothAdapter = bluetoothManager.getAdapter();
            rc = bluetoothAdapter != null;
        }

        if(rc && (bluetoothDevice == null)) {
            if(deviceAddress != null) {
                bluetoothDevice = bluetoothAdapter.getRemoteDevice(deviceAddress);
                rc = bluetoothAdapter != null;
            } else {
                rc = false;
            }
        }

        logi("initialize() :: complete rc = " + rc);

        return rc;
    }

    private void startupConnection() {
        logi("startupConnection() bleManager=" + bleManager);

        boolean success = true;
        int rc = connect();
        if(rc == ERROR_NONE) {
            logi("startupConnection() :: connectMaybeInit() == 0");
            rc = discoverServices();
            if(rc == ERROR_NONE) {

                logi("startupConnection() :: discoverServices() == 0");
                if(registerNotifications(true)) {
                    setNotification(true, 0);
                } else {
                    rc = ERROR_UNKNOWN_2;
                    success = false;
                }
            } else {
                discoverFailed();
                success = false;
            }
        } else {
            Log.e(TAG, "connect failed");
            success = false;
        }

        if(!success) {
            logi("startupConnection() :: Failed ErrorCode = " + rc);
            if(bleManager != null) {
                reset();
                setNotification(false, rc);
                Toast.makeText(MBApp.getApp(), R.string.bluetooth_pairing_internal_error, Toast.LENGTH_LONG).show();
            }
        }

        logi("startupConnection() :: end");
    }

    private void discoverFailed() {
        Log.e(TAG, "discover failed");
        logi("startupConnection() :: discoverServices() != 0");
    }

    /**
     * Establishes a connection to a bluetooth device.
     *
     * @return Result code number.
     */
    private int connect() {
        int rc = ERROR_UNKNOWN_1;

        if(bleManager != null) {
            rc = bleManager.connect(AUTO_RECONNECT);
            rc = interpretCode(rc, BLEManager.BLE_CONNECTED);
        }

        return rc;
    }

    private List<BluetoothGattService> getServices() {
        if(bleManager != null) {
            return bleManager.getServices();
        }

        return null;
    }
}