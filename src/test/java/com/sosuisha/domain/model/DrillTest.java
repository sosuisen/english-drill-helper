package com.sosuisha.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class DrillTest {
    @Test
    @DisplayName("ドリルは、含まれるセグメントのインデックスのリストを持つ")
    void drill_holds_the_indexes_of_its_segments() {
        var drill = new Drill(List.of(2, 3));

        assertEquals(List.of(2, 3), drill.segmentIndexes());
    }

    @Test
    @DisplayName("セグメントを1つも含まないドリルは作れない")
    void drill_without_segments_cannot_be_created() {
        assertThrows(IllegalArgumentException.class, () -> new Drill(List.of()));
    }

    @Test
    @DisplayName("有音・無音・有音・無音のセグメント列は、[0,1] と [2,3] の2つのドリルになる")
    void sound_silence_sound_silence_becomes_two_drills_of_a_sound_and_a_silence() {
        var segments = segments(
            Segment.Kind.SOUND, Segment.Kind.SILENCE, Segment.Kind.SOUND, Segment.Kind.SILENCE
        );

        assertEquals(
            List.of(new Drill(List.of(0, 1)), new Drill(List.of(2, 3))), Drill.drillsOf(segments)
        );
    }

    @Test
    @DisplayName("先頭が無音のセグメント列（無音・有音・無音）は、先頭の無音を捨てて [1,2] の1つのドリルになる")
    void a_leading_silence_is_dropped() {
        var segments = segments(Segment.Kind.SILENCE, Segment.Kind.SOUND, Segment.Kind.SILENCE);

        assertEquals(List.of(new Drill(List.of(1, 2))), Drill.drillsOf(segments));
    }

    @Test
    @DisplayName("末尾が有音で終わるセグメント列（有音・無音・有音）は、[0,1] と [2] の2つのドリルになる。最後は無音なしのドリル")
    void a_trailing_sound_becomes_a_drill_without_silence() {
        var segments = segments(Segment.Kind.SOUND, Segment.Kind.SILENCE, Segment.Kind.SOUND);

        assertEquals(
            List.of(new Drill(List.of(0, 1)), new Drill(List.of(2))), Drill.drillsOf(segments)
        );
    }

    @Test
    @DisplayName("空のセグメント列は、空のドリルリストになる")
    void no_segments_give_no_drills() {
        assertEquals(List.of(), Drill.drillsOf(List.of()));
    }

    /** Segments of one second each, numbered from zero, with the given kinds. */
    private static List<Segment> segments(Segment.Kind... kinds) {
        var segments = new ArrayList<Segment>();
        for (var i = 0; i < kinds.length; i++) {
            segments.add(new Segment(i, Duration.ofSeconds(i), Duration.ofSeconds(1), kinds[i]));
        }
        return segments;
    }
}
