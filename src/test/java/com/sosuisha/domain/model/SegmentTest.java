package com.sosuisha.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Duration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SegmentTest {
    @Test
    @DisplayName("セグメントは、区間長と、有音か無音かの区別を持つ")
    void segment_has_a_duration_and_a_kind_of_sound_or_silence() {
        var segment = new Segment(Duration.ofMillis(500), Segment.Kind.SILENCE);

        assertEquals(Duration.ofMillis(500), segment.duration());
        assertEquals(Segment.Kind.SILENCE, segment.kind());
    }
}
