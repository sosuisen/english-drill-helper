package com.sosuisha.domain.model;

import java.time.Duration;
import java.util.Objects;

/**
 * A part of an audio file that is either sound or silence (see ADR 001).
 *
 * @param duration length of the part
 * @param kind whether the part is sound or silence
 */
public record Segment(Duration duration, Kind kind) {
    /** Whether a segment is sound or silence. */
    public enum Kind {
        SOUND, SILENCE
    }

    /**
     * Creates the segment.
     *
     * @throws NullPointerException if duration or kind is null
     */
    public Segment {
        Objects.requireNonNull(duration, "duration must not be null");
        Objects.requireNonNull(kind, "kind must not be null");
    }
}
