package com.samsung.microbit.core.bluetooth;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.support.annotation.Nullable;
import android.util.Log;

import java.util.List;
import java.util.UUID;

import no.nordicsemi.android.error.GattError;

import static com.samsung.microbit.BuildConfig.DEBUG;

/**
 * Bluetooth low energy manager. Provides methods to establish
 * and manage bluetooth low energy connection.
 */
public class BLEManager {
    private static final String TAG = BLEManager.class.getSimpleName();

    public static final int BLE_DISCONNECTED = 0x0000;
    public static final int BLE_CONNECTED = 0x0001;
    public static final int BLE_SERVICES_DISCOVERED = 0x0002;

    public static final int BLE_ERROR_OK = 0x00000000;
    public static final int BLE_ERROR_FAIL = 0x00010000;
    public static final int BLE_ERROR_TIMEOUT = 0x00020000;

    public static final int BLE_ERROR_NOOP = 0xFFFF0000;
    public static final int BLE_ERROR_NOGATT = -2 & 0xFFFF0000;

    public static final long BLE_WAIT_TIMEOUT = 10000;

    public static final int OP_NOOP = 0;
    public static final int OP_CONNECT = 1;
    public static final int OP_DISCOVER_SERVICES = 2;
    public static final int OP_READ_CHARACTERISTIC = 3;
    public static final int OP_WRITE_CHARACTERISTIC = 4;
    public static final int OP_READ_DESCRIPTOR = 5;
    public static final int OP_WRITE_DESCRIPTOR = 6;
    public static final int OP_CHARACTERISTIC_CHANGED = 7;
    public static final int OP_RELIABLE_WRITE_COMPLETED = 8;
    public static final int OP_READ_REMOTE_RSSI = 9;
    public static final int OP_MTU_CHANGED = 10;

    /**
     * It represents ble device state.
     * Can be one of possible values:
     * {@link BLEManager#BLE_DISCONNECTED}, {@link BLEManager#BLE_CONNECTED}, {@link BLEManager#BLE_SERVICES_DISCOVERED}
     */
    private volatile int bleState = BLE_DISCONNECTED;
    private volatile int error = 0;

    /**
     * It represents ble current operation.
     * Can be one of possible values:
     * {@link BLEManager#OP_NOOP}, {@link BLEManager#OP_CONNECT}, {@link BLEManager#OP_CHARACTERISTIC_CHANGED},
     * {@link BLEManager#OP_DISCOVER_SERVICES}, {@link BLEManager#OP_MTU_CHANGED},
     * {@link BLEManager#OP_READ_CHARACTERISTIC}, {@link BLEManager#OP_READ_DESCRIPTOR},
     * {@link BLEManager#OP_READ_REMOTE_RSSI}, {@link BLEManager#OP_RELIABLE_WRITE_COMPLETED},
     * {@link BLEManager#OP_WRITE_CHARACTERISTIC}, {@link BLEManager#OP_WRITE_DESCRIPTOR},
     */
    private volatile int inBleOp = OP_NOOP;
    private volatile boolean callbackCompleted = false;

    private volatile int rssi;
    private volatile BluetoothGattCharacteristic lastCharacteristic;
    private volatile BluetoothGattDescriptor lastDescriptor;

    private Context context;
    private BluetoothGatt gatt;
    private BluetoothDevice bluetoothDevice;

    private final Object locker = new Object();
    private CharacteristicChangeListener characteristicChangeListener;
    private UnexpectedConnectionEventListener unexpectedDisconnectionListener;

    private int extendedError = 0;

    /**
     * Provides simplified way to log app informational messages.
     *
     * @param message Message to log.
     */
    private static void logi(String message) {
        Log.i(TAG, "### " + Thread.currentThread().getId() + " # " + message);
    }

    public BLEManager(Context context, BluetoothDevice bluetoothDevice, CharacteristicChangeListener
            characteristicChangeListener, UnexpectedConnectionEventListener unexpectedDisconnectionListener) {
        if(DEBUG) {
            logi("start1");
        }

        this.context = context;
        this.bluetoothDevice = bluetoothDevice;
        this.characteristicChangeListener = characteristicChangeListener;
        this.unexpectedDisconnectionListener = unexpectedDisconnectionListener;
    }

    public void setBluetoothDevice(BluetoothDevice bluetoothDevice) {
        this.bluetoothDevice = bluetoothDevice;
    }

    public void setCharacteristicChangeListener(CharacteristicChangeListener characteristicChangeListener) {
        this.characteristicChangeListener = characteristicChangeListener;
    }

    @Nullable
    public BluetoothGattService getService(UUID uuid) {
        if(gatt == null) {
            return null;
        }

        if((bleState & BLE_SERVICES_DISCOVERED) != 0) {
            return gatt.getService(uuid);
        }

        return null;
    }

    @Nullable
    public List<BluetoothGattService> getServices() {
        if(gatt == null) {
            return null;
        }

        if((bleState & BLE_SERVICES_DISCOVERED) != 0) {
            return gatt.getServices();
        }

        return null;
    }

    /**
     * Resets bluetooth GATT connection.
     *
     * @return True if successful.
     */
    public boolean reset() {
        if(DEBUG) {
            logi("reset()");
        }

        synchronized(locker) {
            if(bleState != BLE_DISCONNECTED) {
                disconnect();
            }

            if(bleState != BLE_DISCONNECTED) {
                return false;
            }

            lastCharacteristic = null;
            lastDescriptor = null;
            rssi = 0;
            error = 0;
            inBleOp = OP_NOOP;
            callbackCompleted = false;
            if(gatt != null) {
                if(DEBUG) {
                    logi("reset() :: gatt != null : closing gatt");
                }

                gatt.close();
            }

            gatt = null;
            return true;
        }
    }

    public int getExtendedError() {
        return extendedError;
    }

    /**
     * Allows to establish connection with ability to auto reconnect.
     * <p/>
     * <p/>
     * <strong>Simulating of synchronous request from asynchronous is making in following way:</strong><br/>
     * <ol>
     * <p/>
     * <li>Trigger asynchronous request</li>
     * <li>Wait for defined delay ({@link BLEManager#BLE_WAIT_TIMEOUT}) for asynchronous callback is invoked.</li>
     * <li>If callback is invoked in that time, then just return {@link BLEManager#bleState}.</li>
     * <li>Else mask error using
     * {@link BLEManager#BLE_ERROR_FAIL} and {@link BLEManager#BLE_ERROR_TIMEOUT} and {@link BLEManager#bleState} and
     * return it.</li>
     * </ol>
     *
     * @param autoReconnect Defines if connection is direct, or as soon as it become available.
     * @return Connection result with an appropriate error code if connection is failed.
     */
    public int connect(boolean autoReconnect) {
        int rc = BLE_ERROR_NOOP;

        if(gatt == null) {
            if(DEBUG) {
                logi("connectMaybeInit() :: gatt == null");
            }

            synchronized(locker) {
                if(inBleOp == OP_NOOP) {
                    inBleOp = OP_CONNECT;
                    try {
                        if(DEBUG) {
                            logi("connectMaybeInit() :: bluetoothDevice.connectGatt(context, autoReconnect, bluetoothGattCallback)");
                        }

                        gatt = bluetoothDevice.connectGatt(context, autoReconnect, bluetoothGattCallback);

                        if(gatt == null) {
                            if(DEBUG) {
                                logi("connectGatt failed with AutoReconnect = " + autoReconnect + ". Trying again.. !autoReconnect=" + !autoReconnect);
                            }
                            gatt = bluetoothDevice.connectGatt(context, !autoReconnect, bluetoothGattCallback);
                        }

                        if(gatt != null) {
                            error = 0;
                            locker.wait(BLE_WAIT_TIMEOUT);

                            if(DEBUG) {
                                logi("connectMaybeInit() :: remote device = " + gatt.getDevice().getAddress());
                            }

                            if(!callbackCompleted) {
                                error = (BLE_ERROR_FAIL | BLE_ERROR_TIMEOUT);
                            }

                            rc = error | bleState;
                        }
                    } catch(InterruptedException e) {
                        Log.e(TAG, e.toString());
                    }

                    inBleOp = OP_NOOP;
                }
            }
        } else {
            rc = gattConnect();
        }

        if(DEBUG) {
            logi("connectMaybeInit() :: rc = " + rc);
        }
        return rc;
    }

    /**
     * Trigger connection to remote GATT device.
     * <p/>
     * Simulating of synchronous request from asynchronous. For description of that process,
     * and result encoding see {@link BLEManager#connect(boolean)}
     *
     * @return Connection result.
     * @see BLEManager#connect(boolean)
     */
    private int gattConnect() {
        if(DEBUG) {
            logi("gattConnect() :: start");
        }

        int rc = BLE_ERROR_NOOP;

        synchronized(locker) {
            if(gatt != null && inBleOp == OP_NOOP) {
                if(DEBUG) {
                    logi("gattConnect() :: gatt != null");
                }

                inBleOp = OP_CONNECT;
                error = 0;
                try {
                    if(bleState == BLE_DISCONNECTED) {
                        if(DEBUG) {
                            logi("gattConnect() :: gatt.connectMaybeInit()");
                        }

                        callbackCompleted = false;

                        boolean result = gatt.connect();
                        logi("gatt.connectMaybeInit() returns = " + result);
                        locker.wait(BLE_WAIT_TIMEOUT);

                        if(DEBUG) {
                            logi("gattConnect() :: remote device = " + gatt.getDevice().getAddress());
                        }

                        if(!callbackCompleted) {
                            logi("BLE_ERROR_FAIL | BLE_ERROR_TIMEOUT");
                            error = (BLE_ERROR_FAIL | BLE_ERROR_TIMEOUT);
                        }
                        rc = error | bleState;
                    }
                } catch(InterruptedException e) {
                    Log.e(TAG, e.toString());
                }

                inBleOp = OP_NOOP;
            }
        }

        if(DEBUG) {
            logi("gattConnect() :: rc = " + rc);
        }

        return rc;
    }

    /**
     * Trigger closing active connection with remote GATT.
     * <p/>
     * Simulating of synchronous request from asynchronous. For description of that process,
     * and result encoding see {@link BLEManager#connect(boolean)}
     *
     * @return Disconnection result.
     * @see BLEManager#connect(boolean)
     * @see BLEManager#discoverServices()
     */
    public int disconnect() {
        if(DEBUG) {
            logi("disconnect() :: start");
        }

        int rc = BLE_ERROR_NOOP;

        synchronized(locker) {
            if(gatt != null && inBleOp == OP_NOOP) {

                inBleOp = OP_CONNECT;
                try {
                    error = 0;
                    if(bleState != BLE_DISCONNECTED) {
                        callbackCompleted = false;
                        gatt.disconnect();
                        locker.wait(BLE_WAIT_TIMEOUT);
                        if(!callbackCompleted) {
                            error = (BLE_ERROR_FAIL | BLE_ERROR_TIMEOUT);
                        }
                    }

                    rc = error | bleState;
                } catch(InterruptedException e) {
                    Log.e(TAG, e.toString());
                }

                inBleOp = OP_NOOP;
            }
        }

        if(DEBUG) {
            logi("disconnect() :: rc = " + rc);
        }

        return rc;
    }

    /**
     * Trigger service discovering.
     * <p/>
     * Simulating of synchronous request from asynchronous. For description of that process,
     * and result encoding see {@link BLEManager#connect(boolean)}
     * <p/>
     * After successful discovering, you can search of device services via {@link #getServices()} method.
     *
     * @return Result of discovering.
     * @see BLEManager#connect(boolean)
     */
    public int discoverServices() {
        if(DEBUG) {
            logi("discoverServices() :: start");
        }

        int rc = BLE_ERROR_NOOP;
        synchronized(locker) {
            if(gatt != null && inBleOp == OP_NOOP) {

                inBleOp = OP_DISCOVER_SERVICES;
                error = 0;
                try {
                    callbackCompleted = false;
                    if(gatt.discoverServices()) {
                        locker.wait(BLE_WAIT_TIMEOUT);
                        if(!callbackCompleted) {
                            error = (BLE_ERROR_FAIL | BLE_ERROR_TIMEOUT);
                        }

                        rc = error | bleState;
                    }
                } catch(InterruptedException e) {
                    Log.e(TAG, e.toString());
                }

                inBleOp = OP_NOOP;
            }
        }

        if(DEBUG) {
            logi("discoverServices() :: end : rc = " + rc);
        }

        return rc;
    }

    public boolean isConnected() {
        return bleState == BLE_CONNECTED || bleState == BLE_SERVICES_DISCOVERED || bleState == (BLE_CONNECTED |
                BLE_SERVICES_DISCOVERED);
    }

    /**
     * Write descriptor to connected GATT device.
     * <p/>
     * Simulating of synchronous request from asynchronous. For description of that process,
     * and result encoding see {@link BLEManager#connect(boolean)}
     *
     * @param descriptor Descriptor for writing to remote GATT.
     * @return Result of descriptor writing.
     * @see BLEManager#connect(boolean)
     */
    public int writeDescriptor(BluetoothGattDescriptor descriptor) {
        if(DEBUG) {
            logi("writeDescriptor() :: start");
        }

        int rc = BLE_ERROR_NOOP;

        synchronized(locker) {
            if(gatt != null && inBleOp == OP_NOOP) {

                inBleOp = OP_WRITE_DESCRIPTOR;
                lastDescriptor = null;
                error = 0;
                try {
                    if(gatt.writeDescriptor(descriptor)) {
                        callbackCompleted = false;
                        locker.wait(BLE_WAIT_TIMEOUT);
                        if(!callbackCompleted) {
                            error = (BLE_ERROR_FAIL | BLE_ERROR_TIMEOUT);
                        }

                        rc = error | bleState;
                    }

                } catch(InterruptedException e) {
                    Log.e(TAG, e.toString());
                }

                inBleOp = OP_NOOP;
            }
        }

        if(DEBUG) {
            logi("writeDescriptor() :: end : rc = " + rc);
        }

        return rc;
    }

    /**
     * Reads descriptor from connected GATT device.
     * <p/>
     * Simulating of synchronous request from asynchronous. For description of that process,
     * and result encoding see {@link BLEManager#connect(boolean)}
     *
     * @param descriptor Descriptor for read to from remote GATT.
     * @return Result of descriptor reading.
     * @see BLEManager#connect(boolean)
     */
    public int readDescriptor(BluetoothGattDescriptor descriptor) {
        if(DEBUG) {
            logi("readDescriptor() :: start");
        }

        int rc = BLE_ERROR_NOOP;

        synchronized(locker) {
            if(gatt != null && inBleOp > OP_NOOP) {

                inBleOp = OP_READ_DESCRIPTOR;
                lastDescriptor = null;
                error = 0;
                try {
                    callbackCompleted = false;
                    if(gatt.readDescriptor(descriptor)) {
                        locker.wait(BLE_WAIT_TIMEOUT);
                        if(!callbackCompleted) {
                            error = (BLE_ERROR_FAIL | BLE_ERROR_TIMEOUT);
                        }

                        rc = error | bleState;
                    }
                } catch(InterruptedException e) {
                    Log.e(TAG, e.toString());
                }

                inBleOp = OP_NOOP;
            }
        }

        if(DEBUG) {
            logi("readDescriptor() :: end : rc = " + rc);
        }

        return rc;
    }

    /**
     * Write characteristic to connected GATT device.
     * <p/>
     * Simulating of synchronous request from asynchronous. For description of that process,
     * and result encoding see {@link BLEManager#connect(boolean)}
     *
     * @param characteristic Characteristic for writing to remote GATT.
     * @return Result of characteristic writing.
     * @see BLEManager#connect(boolean)
     */
    public int writeCharacteristic(BluetoothGattCharacteristic characteristic) {
        if(DEBUG) {
            logi("writeCharacteristic() :: start");
        }

        int rc = BLE_ERROR_NOOP;

        synchronized(locker) {
            if(gatt != null && inBleOp == OP_NOOP) {
                inBleOp = OP_WRITE_CHARACTERISTIC;
                lastCharacteristic = null;
                error = 0;
                try {
                    callbackCompleted = false;
                    if(gatt.writeCharacteristic(characteristic)) {
                        locker.wait(BLE_WAIT_TIMEOUT);
                        if(!callbackCompleted) {
                            error = (BLE_ERROR_FAIL | BLE_ERROR_TIMEOUT);
                        }

                        rc = error | bleState;
                    } else {
                        if(DEBUG) {
                            logi("writeCharacteristic() :: failed");
                        }
                    }


                } catch(InterruptedException e) {
                    Log.e(TAG, e.toString());
                }

                inBleOp = OP_NOOP;
            } else {
                logi("Couldn't write to characteristic");
            }

        }

        if(DEBUG) {
            logi("writeCharacteristic() :: end : rc = " + rc);
        }

        return rc;
    }

    /**
     * Read characteristic from connected GATT device.
     * <p/>
     * Simulating of synchronous request from asynchronous. For description of that process,
     * and result encoding see {@link BLEManager#connect(boolean)}
     *
     * @param characteristic Characteristic for reading from remote GATT.
     * @return Result of characteristic reading.
     * @see BLEManager#connect(boolean)
     */
    public int readCharacteristic(BluetoothGattCharacteristic characteristic) {
        if(DEBUG) {
            logi("readCharacteristic() :: start");
        }

        int rc = BLE_ERROR_NOOP;

        synchronized(locker) {
            if(gatt != null && inBleOp == OP_NOOP) {

                inBleOp = OP_READ_CHARACTERISTIC;
                lastCharacteristic = null;
                error = 0;
                int bleState = this.bleState;
                try {
                    callbackCompleted = false;
                    if(gatt.readCharacteristic(characteristic)) {
                        locker.wait(BLE_WAIT_TIMEOUT);
                        if(!callbackCompleted) {
                            error = (BLE_ERROR_FAIL | BLE_ERROR_TIMEOUT);
                        } else {
                            bleState = this.bleState;
                        }

                        rc = error | bleState;
                    }
                } catch(InterruptedException e) {
                    Log.e(TAG, e.toString());
                }

                inBleOp = OP_NOOP;
            }
        }

        if(DEBUG) {
            logi("readCharacteristic() :: end : rc = " + rc);
        }

        return rc;
    }

    public BluetoothGattCharacteristic getLastCharacteristic() {
        return lastCharacteristic;
    }

    /**
     * Enable or disable notifications/indications for a given characteristic.
     *
     * @param characteristic The characteristic for which to enable notifications.
     * @param descriptor     Bluetooth GATT descriptor.
     * @param enable         Enable or disable notification.
     * @return Result of enabling notifications.
     */
    public int enableCharacteristicNotification(BluetoothGattCharacteristic characteristic, BluetoothGattDescriptor
            descriptor, boolean enable) {
        if(gatt == null) {
            return BLE_ERROR_NOOP;
        }
        int rc = BLE_ERROR_NOOP;

        synchronized(locker) {
            error = 0;

            if(gatt.setCharacteristicNotification(characteristic, enable)) {
                logi("characteristic notif success");
                rc = error | bleState;

                descriptor.setValue(enable ? BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE :
                        BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);
            }
        }

        return writeDescriptor(descriptor) | rc;
    }

    /**
     * Callback for handling bluetooth GATT interaction.
     */
    private final BluetoothGattCallback bluetoothGattCallback = new BluetoothGattCallback() {

        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);

            if(DEBUG) {
                logi("BluetoothGattCallback.onConnectionStateChange() :: start : status = " + status + " newState = " +
                        "" + newState);
            }

            int state = BLE_DISCONNECTED;
            int error = 0;

            boolean gattForceClosed = false;

            switch(status) {
                case BluetoothGatt.GATT_SUCCESS: {
                    if(newState == BluetoothProfile.STATE_CONNECTED) {
                        state = BLE_CONNECTED;
                    } else if(newState == BluetoothProfile.STATE_DISCONNECTED) {
                        state = BLE_DISCONNECTED;
                        if(gatt != null) {
                            if(DEBUG) {
                                logi("onConnectionStateChange() :: gatt != null : closing gatt");
                            }

                            gatt.disconnect();
                            gatt.close();
                            gattForceClosed = true;
                            if(BLEManager.this.gatt.getDevice().getAddress().equals(gatt.getDevice().getAddress())) {
                                BLEManager.this.gatt = null;
                            }
                        }
                    }
                }
                break;
                default:
                    Log.e(TAG, "Connection error: " + GattError.parseConnectionError(status));
                    break;
            }

            if(status != BluetoothGatt.GATT_SUCCESS) {
                state = BLE_DISCONNECTED;
                error = BLE_ERROR_FAIL;
            }

            synchronized(locker) {
                if(inBleOp == OP_CONNECT) {
                    if(DEBUG) {
                        logi("BluetoothGattCallback.onConnectionStateChange() :: inBleOp == OP_CONNECT");
                    }

                    if(state != (bleState & BLE_CONNECTED)) {
                        bleState = state;
                    }
                    callbackCompleted = true;
                    BLEManager.this.error = error;
                    extendedError = status;
                    locker.notify();
                } else {
                    if(DEBUG) {
                        logi("onConnectionStateChange() :: inBleOp != OP_CONNECT");
                    }

                    bleState = state;
                    unexpectedDisconnectionListener.handleConnectionEvent(bleState, gattForceClosed);
                }

                if(DEBUG) {
                    logi("BluetoothGattCallback.onConnectionStateChange() :: end");
                }
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);

            int state = BLE_SERVICES_DISCOVERED;

            synchronized(locker) {
                if(DEBUG) {
                    logi("BluetoothGattCallback.onServicesDiscovered() :: start : status = " + status);
                }

                if(inBleOp == OP_DISCOVER_SERVICES) {
                    if(DEBUG) {
                        logi("BluetoothGattCallback.onServicesDiscovered() :: inBleOp == OP_DISCOVER_SERVICES");
                    }

                    if(status == BluetoothGatt.GATT_SUCCESS) {
                        bleState |= state;

                    } else {
                        bleState &= (~state);
                    }

                    callbackCompleted = true;
                    locker.notify();
                }

                if(DEBUG) {
                    logi("BluetoothGattCallback.onServicesDiscovered() :: end");
                }
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicRead(gatt, characteristic, status);

            synchronized(locker) {
                if(DEBUG) {
                    logi("BluetoothGattCallback.onCharacteristicRead() :: start : status = " + status);
                }

                if(inBleOp == OP_READ_CHARACTERISTIC) {
                    if(DEBUG) {
                        logi("BluetoothGattCallback.onCharacteristicRead() :: inBleOp == OP_READ_CHARACTERISTIC");
                    }

                    if(status == BluetoothGatt.GATT_SUCCESS) {
                        error = BLE_ERROR_OK;
                    } else {
                        error = BLE_ERROR_FAIL;
                    }

                    lastCharacteristic = characteristic;
                    callbackCompleted = true;
                    locker.notify();
                }

                if(DEBUG) {
                    logi("BluetoothGattCallback.onCharacteristicRead() :: end");
                }
            }
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicWrite(gatt, characteristic, status);

            synchronized(locker) {
                if(DEBUG) {
                    logi("BluetoothGattCallback.onCharacteristicWrite() :: start : status = " + status);
                }

                if(inBleOp == OP_WRITE_CHARACTERISTIC) {
                    if(DEBUG) {
                        logi("BluetoothGattCallback.onCharacteristicWrite() :: inBleOp == OP_WRITE_CHARACTERISTIC");
                    }

                    if(status == BluetoothGatt.GATT_SUCCESS) {
                        error = BLE_ERROR_OK;
                    } else {
                        error = BLE_ERROR_FAIL;
                    }

                    lastCharacteristic = characteristic;
                    callbackCompleted = true;
                    locker.notify();
                }

                if(DEBUG) {
                    logi("BluetoothGattCallback.onCharacteristicWrite() :: end");
                }
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            if(DEBUG) {
                logi("BluetoothGattCallback.onCharacteristicChanged() :: start");
            }

            super.onCharacteristicChanged(gatt, characteristic);
            characteristicChangeListener.onCharacteristicChanged(gatt, characteristic);

            if(DEBUG) {
                logi("BluetoothGattCallback.onCharacteristicChanged() :: end");
            }
        }

        @Override
        public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            super.onDescriptorRead(gatt, descriptor, status);

            synchronized(locker) {
                if(DEBUG) {
                    logi("BluetoothGattCallback.onDescriptorRead() :: start : status = " + status);
                }

                if(inBleOp == OP_READ_DESCRIPTOR) {
                    if(DEBUG) {
                        logi("BluetoothGattCallback.onDescriptorRead() :: inBleOp == OP_READ_DESCRIPTOR");
                    }
                    if(status == BluetoothGatt.GATT_SUCCESS) {
                        error = BLE_ERROR_OK;
                    } else {
                        error = BLE_ERROR_FAIL;
                    }

                    lastDescriptor = descriptor;
                    callbackCompleted = true;
                    locker.notify();
                }

                if(DEBUG) {
                    logi("BluetoothGattCallback.onDescriptorRead() :: end");
                }
            }
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            super.onDescriptorWrite(gatt, descriptor, status);

            synchronized(locker) {
                if(DEBUG) {
                    logi("BluetoothGattCallback.onDescriptorWrite() :: start : status = " + status);
                }

                if(inBleOp == OP_WRITE_DESCRIPTOR) {
                    if(DEBUG) {
                        logi("BluetoothGattCallback.onDescriptorWrite() :: inBleOp == OP_WRITE_DESCRIPTOR");
                    }

                    if(status == BluetoothGatt.GATT_SUCCESS) {
                        error = BLE_ERROR_OK;
                    } else {
                        error = BLE_ERROR_FAIL;
                    }

                    lastDescriptor = descriptor;
                    callbackCompleted = true;
                    locker.notify();
                }

                if(DEBUG) {
                    logi("BluetoothGattCallback.onDescriptorWrite() :: end");
                }
            }
        }

        @Override
        public void onReliableWriteCompleted(BluetoothGatt gatt, int status) {
            super.onReliableWriteCompleted(gatt, status);

            synchronized(locker) {
                if(DEBUG) {
                    logi("BluetoothGattCallback.onReliableWriteCompleted() :: start");
                }

                if(status == BluetoothGatt.GATT_SUCCESS) {
                    error = BLE_ERROR_OK;
                } else {
                    error = BLE_ERROR_FAIL;
                }

                callbackCompleted = true;
                locker.notify();

                if(DEBUG) {
                    logi("BluetoothGattCallback.onReliableWriteCompleted() :: end");
                }
            }
        }

        @Override
        public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
            super.onReadRemoteRssi(gatt, rssi, status);

            synchronized(locker) {
                if(DEBUG) {
                    logi("BluetoothGattCallback.onReadRemoteRssi() :: start");
                }

                if(inBleOp == OP_READ_REMOTE_RSSI) {
                    if(DEBUG) {
                        logi("BluetoothGattCallback.onReadRemoteRssi() :: inBleOp == OP_READ_REMOTE_RSSI");
                    }

                    if(status == BluetoothGatt.GATT_SUCCESS) {
                        error = BLE_ERROR_OK;
                    } else {
                        error = BLE_ERROR_FAIL;
                    }

                    BLEManager.this.rssi = rssi;
                    callbackCompleted = true;
                    locker.notify();
                }

                if(DEBUG) {
                    logi("BluetoothGattCallback.onMtuChanged() :: end");
                }
            }
        }

        @Override
        public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
            super.onMtuChanged(gatt, mtu, status);

            synchronized(locker) {
                if(DEBUG) {
                    logi("BluetoothGattCallback.onMtuChanged() :: start");
                }

                if(status == BluetoothGatt.GATT_SUCCESS) {
                    error = BLE_ERROR_OK;
                } else {
                    error = BLE_ERROR_FAIL;
                }

                if(DEBUG) {
                    logi("BluetoothGattCallback.onMtuChanged() :: end");
                }
            }
        }
    };
}