package com.sosuisha.domain.model;

import java.time.Duration;
import java.util.Objects;

/**
 * Parameters of the silence detection (see ADR 001 and ADR 003). The PCM is
 * cut into windows of {@code windowWidth}; a window whose loudness is below
 * {@code silenceThresholdDbfs} is silent; silent windows that last at least
 * {@code minSilenceDuration} form a silence segment.
 *
 * @param windowWidth width of one window, longer than zero
 * @param silenceThresholdDbfs loudness below which a window is silent, in dBFS
 * @param minSilenceDuration shortest run of silent windows that counts as
 *        silence, longer than zero
 */
public record SegmentDetectionParameters(
    Duration windowWidth, double silenceThresholdDbfs, Duration minSilenceDuration) {
    /** The defaults decided in ADR 003 for drills with pauses for shadowing. */
    public static final SegmentDetectionParameters DEFAULT =
        new SegmentDetectionParameters(Duration.ofMillis(20), -45.0, Duration.ofSeconds(1));

    /**
     * Creates the parameters.
     *
     * @throws NullPointerException if windowWidth or minSilenceDuration is null
     * @throws IllegalArgumentException if windowWidth or minSilenceDuration is zero or negative
     */
    public SegmentDetectionParameters {
        Objects.requireNonNull(windowWidth, "windowWidth must not be null");
        Objects.requireNonNull(minSilenceDuration, "minSilenceDuration must not be null");
        requirePositive(windowWidth, "windowWidth");
        requirePositive(minSilenceDuration, "minSilenceDuration");
    }

    private static void requirePositive(Duration duration, String name) {
        if (duration.isZero() || duration.isNegative()) {
            throw new IllegalArgumentException(name + " must be positive: " + duration);
        }
    }
}
