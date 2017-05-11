package com.samsung.microbit.data.constants;

import android.support.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import static com.samsung.microbit.data.constants.ServiceIds.*;

/**
 * Used for detecting client of current message.
 * Messages are sending from service to client handler.
 * Based on {@link android.os.Message#arg2} and {@link ServiceIds} values,
 * handler detect which service finally receive incomming message.
 */
@Retention(RetentionPolicy.RUNTIME)
@IntDef({SERVICE_NONE, SERVICE_PLUGIN, SERVICE_BLE})
public @interface ServiceIds {
    int SERVICE_NONE = 0;
    int SERVICE_PLUGIN = 1;
    int SERVICE_BLE = 2;
}
