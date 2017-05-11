package com.samsung.microbit.plugin;

import com.samsung.microbit.data.model.CmdArg;

//TODO: consider to use somewhere or remove
public class FilePlugin implements AbstractPlugin {

    //File plugin action
    public static final int DOWNLOAD = 0;

    @Override
    public void handleEntry(CmdArg cmd) {
        switch(cmd.getCMD()) {
            case DOWNLOAD:
                //TODO CALL THE DOWNLOAD FUNCTION HERE
                break;
        }
    }

    @Override
    public void destroy() {

    }
}
