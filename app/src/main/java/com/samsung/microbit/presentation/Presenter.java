package com.samsung.microbit.presentation;

/**
 * Provides common methods for presenters.
 */
public interface Presenter {
    /**
     * Start some process.
     */
    void start();

    /**
     * Stop some process.
     */
    void stop();

    /**
     * Releases resources to prevent memory leaks.
     */
    void destroy();
}
