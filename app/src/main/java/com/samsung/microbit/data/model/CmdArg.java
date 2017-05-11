package com.samsung.microbit.data.model;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Represents class, that consists of int command code
 * and a String value.
 */
public class CmdArg implements Parcelable {

    private int cmd;
    private String value;

    public CmdArg(int cmd, String val) {
        this.cmd = cmd;
        this.value = val;
    }

    public int getCMD() {
        return this.cmd;
    }

    public String getValue() {
        return this.value;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(cmd);
        dest.writeString(value);
    }

    public static final Creator<CmdArg> CREATOR = new Creator<CmdArg>() {
        @Override
        public CmdArg createFromParcel(Parcel source) {
            return new CmdArg(source.readInt(), source.readString());
        }

        @Override
        public CmdArg[] newArray(int size) {
            return new CmdArg[size];
        }
    };
}
