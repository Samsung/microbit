package com.samsung.microbit.data.constants;

import android.bluetooth.BluetoothGattCharacteristic;

/**
 * Contains gatt characteristic formats. For example,
 * set type of the characteristic that going to be written.
 */
public class GattFormats {
    private GattFormats() {
    }

    public static final int FORMAT_UINT8 = BluetoothGattCharacteristic.FORMAT_UINT8;
    public static final int FORMAT_UINT16 = BluetoothGattCharacteristic.FORMAT_UINT16;
    public static final int FORMAT_UINT32 = BluetoothGattCharacteristic.FORMAT_UINT32;

    public static final int FORMAT_SINT8 = BluetoothGattCharacteristic.FORMAT_SINT8;
    public static final int FORMAT_SINT16 = BluetoothGattCharacteristic.FORMAT_SINT16;
    public static final int FORMAT_SINT32 = BluetoothGattCharacteristic.FORMAT_SINT32;
}
