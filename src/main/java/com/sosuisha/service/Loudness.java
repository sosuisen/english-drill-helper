package com.sosuisha.service;

import java.util.Objects;

/**
 * Measures the loudness of a window of 16-bit PCM samples (see ADR 003).
 */
public final class Loudness {
    private static final double FULL_SCALE = Short.MAX_VALUE;

    private Loudness() {}

    /**
     * Converts the RMS of the samples to dBFS. The samples of all channels
     * are given together in one array.
     *
     * @param samples 16-bit PCM samples of one window, at least one
     * @return the loudness in dBFS, 0 at full scale
     * @throws NullPointerException if samples is null
     * @throws IllegalArgumentException if samples is empty
     */
    public static double dbfsOf(short[] samples) {
        Objects.requireNonNull(samples, "samples must not be null");
        if (samples.length == 0) {
            throw new IllegalArgumentException("samples must not be empty");
        }
        var sumOfSquares = 0.0;
        for (var sample : samples) {
            sumOfSquares += (double) sample * sample;
        }
        var rms = Math.sqrt(sumOfSquares / samples.length);
        return 20 * Math.log10(rms / FULL_SCALE);
    }
}
