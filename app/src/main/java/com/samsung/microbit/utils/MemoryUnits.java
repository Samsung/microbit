package com.samsung.microbit.utils;

/**
 * Utility that provides functionality to work with memory,
 * such as converting bytes to kilobytes, megabytes, gigabytes
 * and visa versa.
 */
public abstract class MemoryUnits {
    private MemoryUnits() {
    }

    private static final long CONVERSION_VALUE = 1024;

    public abstract long toBytes(long value);

    public abstract long toKilobytes(long value);

    public abstract long toMegabytes(long value);

    public abstract long toGigabytes(long value);

    public abstract long toTerabytes(long value);

    /**
     * Represents a byte measure.
     * Allows to convert to another measures.
     */
    public static class Bytes extends MemoryUnits {

        @Override
        public long toBytes(long value) {
            return value;
        }

        @Override
        public long toKilobytes(long value) {
            return value / CONVERSION_VALUE;
        }

        @Override
        public long toMegabytes(long value) {
            return value / (CONVERSION_VALUE * CONVERSION_VALUE);
        }

        @Override
        public long toGigabytes(long value) {
            return value / (CONVERSION_VALUE * CONVERSION_VALUE * CONVERSION_VALUE);
        }

        @Override
        public long toTerabytes(long value) {
            return value / (CONVERSION_VALUE * CONVERSION_VALUE * CONVERSION_VALUE *
                    CONVERSION_VALUE);
        }

        public static MemoryUnits instance() {
            return new Bytes();
        }
    }

    /**
     * Represents a kilobyte measure.
     * Allows to convert to another measures.
     */
    public static class Kilobytes extends MemoryUnits {
        @Override
        public long toBytes(long value) {
            return value * CONVERSION_VALUE;
        }

        @Override
        public long toKilobytes(long value) {
            return value;
        }

        @Override
        public long toMegabytes(long value) {
            return value / CONVERSION_VALUE;
        }

        @Override
        public long toGigabytes(long value) {
            return value / (CONVERSION_VALUE * CONVERSION_VALUE);
        }

        @Override
        public long toTerabytes(long value) {
            return value / (CONVERSION_VALUE * CONVERSION_VALUE * CONVERSION_VALUE);
        }

        public static MemoryUnits instance() {
            return new Kilobytes();
        }
    }

    /**
     * Represents a megabyte measure.
     * Allows to convert to another measures.
     */
    public static class Megabytes extends MemoryUnits {
        @Override
        public long toBytes(long value) {
            return value * CONVERSION_VALUE * CONVERSION_VALUE;
        }

        @Override
        public long toKilobytes(long value) {
            return value * CONVERSION_VALUE;
        }

        @Override
        public long toMegabytes(long value) {
            return value;
        }

        @Override
        public long toGigabytes(long value) {
            return value / CONVERSION_VALUE;
        }

        @Override
        public long toTerabytes(long value) {
            return value / (CONVERSION_VALUE * CONVERSION_VALUE);
        }

        public static MemoryUnits instance() {
            return new Megabytes();
        }
    }

    /**
     * Represents a gigabyte measure.
     * Allows to convert to another measures.
     */
    public static class Gigabytes extends MemoryUnits {
        @Override
        public long toBytes(long value) {
            return value * CONVERSION_VALUE * CONVERSION_VALUE * CONVERSION_VALUE;
        }

        @Override
        public long toKilobytes(long value) {
            return value * CONVERSION_VALUE * CONVERSION_VALUE;
        }

        @Override
        public long toMegabytes(long value) {
            return value * CONVERSION_VALUE;
        }

        @Override
        public long toGigabytes(long value) {
            return value;
        }

        @Override
        public long toTerabytes(long value) {
            return value / CONVERSION_VALUE;
        }

        public static MemoryUnits instance() {
            return new Gigabytes();
        }
    }

    /**
     * Represents a terabyte measure.
     * Allows to convert to another measures.
     */
    public static class Terabytes extends MemoryUnits {
        @Override
        public long toBytes(long value) {
            return value * CONVERSION_VALUE * CONVERSION_VALUE * CONVERSION_VALUE *
                    CONVERSION_VALUE;
        }

        @Override
        public long toKilobytes(long value) {
            return value * CONVERSION_VALUE * CONVERSION_VALUE * CONVERSION_VALUE;
        }

        @Override
        public long toMegabytes(long value) {
            return value * CONVERSION_VALUE * CONVERSION_VALUE;
        }

        @Override
        public long toGigabytes(long value) {
            return value * CONVERSION_VALUE;
        }

        @Override
        public long toTerabytes(long value) {
            return value;
        }

        public static MemoryUnits instance() {
            return new Terabytes();
        }
    }
}
