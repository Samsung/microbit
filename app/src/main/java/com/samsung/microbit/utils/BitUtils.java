package com.samsung.microbit.utils;

/**
 * Represent an utility that provides methods to work with
 * bitwise operations.
 */
//TODO: consider to use somewhere or remove
public class BitUtils {

    private BitUtils() {
    }

    // bit position to value mask
    public static int getBitMask(int x) {
        return (0x01 << x);
    }

    // multiple bit positions to value mask
    public static int getBitMask(int[] x) {
        int rc = 0;
        for(int xVal : x) {
            rc |= getBitMask(xVal);
        }

        return rc;
    }

    public int setBit(int v, int x) {
        v |= getBitMask(x);
        return v;
    }

    public int setBits(int v, int[] x) {
        v |= getBitMask(x);
        return v;
    }

    public static int clearBit(int v, int x) {
        v &= ~getBitMask(x);
        return v;
    }

    public static int clearBits(int v, int[] x) {
        v &= ~getBitMask(x);
        return v;
    }

    public static int maskBit(int v, int x) {
        v &= getBitMask(x);
        return v;
    }

    public static int maskBits(int v, int[] x) {
        v &= getBitMask(x);
        return v;
    }
}
