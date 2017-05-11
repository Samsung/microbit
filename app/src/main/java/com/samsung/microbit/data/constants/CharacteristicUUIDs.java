package com.samsung.microbit.data.constants;

import com.samsung.microbit.service.BLEService;
import com.samsung.microbit.service.PluginService;
import com.samsung.microbit.utils.Utils;

import java.util.UUID;

/**
 * Contains characteristic universally unique identifiers,
 * that used by services {@link PluginService PluginService},
 * {@link BLEService BLEService}
 */
public class CharacteristicUUIDs {
    private CharacteristicUUIDs() {
    }

    public static final UUID ES_CLIENT_REQUIREMENTS = Utils.makeUUID(UUIDs.MICROBIT_BASE_UUID_STR, 0x023C4);
    public static final UUID ES_MICROBIT_REQUIREMENTS = Utils.makeUUID(UUIDs.MICROBIT_BASE_UUID_STR, 0x0B84C);

    public static final UUID ES_MICROBIT_EVENT = Utils.makeUUID(UUIDs.MICROBIT_BASE_UUID_STR, 0x09775);
    //Events going to the micro:bit. For this  ES_MICROBIT_REQUIREMENTS needs to be set properly.

    public static final UUID ES_CLIENT_EVENT = Utils.makeUUID(UUIDs.MICROBIT_BASE_UUID_STR, 0x05404);
    //Events Coming from micro:bit. For this  ES_CLIENT_REQUIREMENTS needs to be set properly.

    public static final UUID FIRMWARE_REVISION_UUID = new UUID(0x00002A2600001000L, 0x800000805F9B34FBL);

}
