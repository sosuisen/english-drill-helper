package com.sosuisha.domain.model;

import java.util.Objects;

/**
 * Decoded audio as 16-bit PCM samples. The samples of all channels are
 * interleaved in one array, so a frame of a stereo file takes two elements.
 * As with every record that holds an array, equality is by reference of the
 * array, not by its content.
 *
 * @param sampleRate frames per second, such as 44100
 * @param channels number of channels, 1 for mono
 * @param samples 16-bit PCM samples, interleaved by channel
 */
public record PcmAudio(int sampleRate, int channels, short[] samples) {
    /**
     * Creates the PCM audio.
     *
     * @throws NullPointerException if samples is null
     * @throws IllegalArgumentException if sampleRate or channels is zero or negative
     */
    public PcmAudio {
        Objects.requireNonNull(samples, "samples must not be null");
        if (sampleRate <= 0) {
            throw new IllegalArgumentException("sampleRate must be positive: " + sampleRate);
        }
        if (channels <= 0) {
            throw new IllegalArgumentException("channels must be positive: " + channels);
        }
    }
}
