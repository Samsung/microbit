package com.samsung.microbit.utils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Utility that provides methods to work with input/output operations.
 */
public class IOUtils {

    private static final int BUFFER_SIZE = 4096;

    private IOUtils() {
    }

    /**
     * Copies data from one input stream to another.
     *
     * @param src  Source stream from which data copy.
     * @param dest Destination stream where data should be copied.
     * @return Number of total bytes copied.
     * @throws IOException
     */
    public static long copy(InputStream src, OutputStream dest) throws IOException {
        byte[] buffer = new byte[BUFFER_SIZE];

        long countBytes = 0;

        int count;

        while((count = src.read(buffer)) > 0) {
            dest.write(buffer, 0, count);
            countBytes += count;
        }

        src.close();
        dest.flush();
        dest.close();

        return countBytes;
    }
}
