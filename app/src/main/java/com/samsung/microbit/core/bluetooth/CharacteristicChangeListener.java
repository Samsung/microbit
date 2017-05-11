package com.samsung.microbit.core.bluetooth;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;

public interface CharacteristicChangeListener {
    /**
     * Callback triggered as a result of characteristic changed.
     *
     * @param gatt           GATT client the characteristic is associated with
     * @param characteristic Updated characteristic
     */
    void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic);
}
