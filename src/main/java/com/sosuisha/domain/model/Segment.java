package com.sosuisha.domain.model;

import java.time.Duration;
import java.util.Objects;

/**
 * A part of an audio file that is either sound or silence (see ADR 001).
 * The segments of a file are numbered from zero in time order, and each one
 * knows where it starts so that playback can jump to it.
 *
 * @param index position of the segment in the file, from zero
 * @param start time from the beginning of the file to the segment, zero or longer
 * @param duration length of the part, longer than zero
 * @param kind whether the part is sound or silence
 */
public record Segment(int index, Duration start, Duration duration, Kind kind) {
    /** Whether a segment is sound or silence. */
    public enum Kind {
        SOUND, SILENCE
    }

    /**
     * Creates the segment.
     *
     * @throws NullPointerException if start, duration, or kind is null
     * @throws IllegalArgumentException if index or start is negative, or if
     *             duration is zero or negative
     */
    public Segment {
        Objects.requireNonNull(start, "start must not be null");
        Objects.requireNonNull(duration, "duration must not be null");
        Objects.requireNonNull(kind, "kind must not be null");
        if (index < 0) {
            throw new IllegalArgumentException("index must not be negative: " + index);
        }
        if (start.isNegative()) {
            throw new IllegalArgumentException("start must not be negative: " + start);
        }
        if (duration.isZero() || duration.isNegative()) {
            throw new IllegalArgumentException("duration must be positive: " + duration);
        }
    }
}
