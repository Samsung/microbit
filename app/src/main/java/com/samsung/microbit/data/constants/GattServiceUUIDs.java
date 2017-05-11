package com.samsung.microbit.data.constants;

import com.samsung.microbit.utils.Utils;

import java.util.UUID;

/**
 * Contains universally unique identifiers for identifying services.
 * You can search GATT device services, and discover it
 * {@link android.bluetooth.BluetoothGattCharacteristic BluetoothGattCharacteristic}s
 */
public class GattServiceUUIDs {
    private GattServiceUUIDs() {
    }

    public static final UUID DEVICE_INFORMATION_SERVICE = new UUID(0x0000180A00001000L, 0x800000805F9B34FBL);
    public static final UUID EVENT_SERVICE = Utils.makeUUID(UUIDs.MICROBIT_BASE_UUID_STR, 0x093af);
}
