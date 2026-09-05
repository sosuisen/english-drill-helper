package com.sosuisha.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Duration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SegmentTest {
    @Test
    @DisplayName("セグメントは、インデックス・開始位置・区間長と、有音か無音かの区別を持つ")
    void segment_has_an_index_a_start_a_duration_and_a_kind_of_sound_or_silence() {
        var segment =
            new Segment(3, Duration.ofMillis(4000), Duration.ofMillis(500), Segment.Kind.SILENCE);

        assertEquals(3, segment.index());
        assertEquals(Duration.ofMillis(4000), segment.start());
        assertEquals(Duration.ofMillis(500), segment.duration());
        assertEquals(Segment.Kind.SILENCE, segment.kind());
    }

    @Test
    @DisplayName("区間長がゼロまたは負のセグメントは作れない")
    void segment_with_zero_or_negative_duration_cannot_be_created() {
        assertThrows(
            IllegalArgumentException.class,
            () -> new Segment(0, Duration.ZERO, Duration.ZERO, Segment.Kind.SOUND)
        );
        assertThrows(
            IllegalArgumentException.class,
            () -> new Segment(0, Duration.ZERO, Duration.ofMillis(-1), Segment.Kind.SOUND)
        );
    }

    @Test
    @DisplayName("インデックスまたは開始位置が負のセグメントは作れない")
    void segment_with_a_negative_index_or_a_negative_start_cannot_be_created() {
        assertThrows(
            IllegalArgumentException.class,
            () -> new Segment(-1, Duration.ZERO, Duration.ofMillis(500), Segment.Kind.SOUND)
        );
        assertThrows(
            IllegalArgumentException.class,
            () -> new Segment(0, Duration.ofMillis(-1), Duration.ofMillis(500), Segment.Kind.SOUND)
        );
    }
}
