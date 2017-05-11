package com.samsung.microbit.core.bluetooth;

public interface UnexpectedConnectionEventListener {
    /**
     * Callback that handles some unexpected connection events.
     *
     * @param event           Unexpected connection event.
     * @param gattForceClosed Defines if connection is force closed.
     */
    void handleConnectionEvent(int event, boolean gattForceClosed);
}

