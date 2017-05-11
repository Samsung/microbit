package com.samsung.microbit.plugin;

import com.samsung.microbit.data.model.CmdArg;

public interface AbstractPlugin {
    /**
     * Handle signal, that become from micro:bit board.
     *
     * @param cmd Command, that should be handled by plugin.
     */
    void handleEntry(CmdArg cmd);

    void destroy();
}