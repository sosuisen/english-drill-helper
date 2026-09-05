package com.sosuisha.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.sosuisha.domain.exception.IrregularUnitException;

class DrillTest {
    @Test
    @DisplayName("ドリルは、ユニット内の番号とターンのリストを持つ")
    void drill_holds_a_number_and_its_turns() {
        var turns = List.of(new Turn(1, 0, Optional.of(1)), new Turn(2, 2, Optional.of(3)));

        var drill = new Drill(1, turns);

        assertEquals(1, drill.number());
        assertEquals(turns, drill.turns());
    }

    @Test
    @DisplayName("番号が1未満のドリルや、ターンを1つも含まないドリルは作れない")
    void drill_with_a_number_below_one_or_without_turns_cannot_be_created() {
        var turns = List.of(new Turn(1, 0, Optional.of(1)));

        assertThrows(IllegalArgumentException.class, () -> new Drill(0, turns));
        assertThrows(IllegalArgumentException.class, () -> new Drill(1, List.of()));
    }

    @Test
    @DisplayName("有音・無音が5組（10セグメント）の列は、1ターンずつの5つのドリルになる。Drill 1 の Turn 1 は [0,1]、Drill 5 の Turn 1 は [8,9]")
    void five_pairs_become_five_drills_of_one_turn_each() {
        var drills = Drill.drillsOf(pairs(5));

        assertEquals(5, drills.size());
        assertEquals(new Drill(1, List.of(new Turn(1, 0, Optional.of(1)))), drills.get(0));
        assertEquals(new Drill(5, List.of(new Turn(1, 8, Optional.of(9)))), drills.get(4));
    }

    @Test
    @DisplayName("有音・無音が10組（20セグメント）の列は、2ターンずつの5つのドリルになる。Drill 1 は [0,1]・[2,3]、Drill 2 は [4,5]・[6,7]")
    void ten_pairs_become_five_drills_of_two_turns_each() {
        var drills = Drill.drillsOf(pairs(10));

        assertEquals(5, drills.size());
        assertEquals(
            new Drill(1, List.of(new Turn(1, 0, Optional.of(1)), new Turn(2, 2, Optional.of(3)))),
            drills.get(0)
        );
        assertEquals(
            new Drill(2, List.of(new Turn(1, 4, Optional.of(5)), new Turn(2, 6, Optional.of(7)))),
            drills.get(1)
        );
    }

    @Test
    @DisplayName("先頭が無音の列（無音 + 有音・無音5組）は、先頭の無音を捨てて5つのドリルになる。Drill 1 の Turn 1 は [1,2]")
    void a_leading_silence_is_dropped() {
        var segments = new ArrayList<Segment>();
        segments.add(new Segment(0, Duration.ZERO, Duration.ofSeconds(1), Segment.Kind.SILENCE));
        segments.addAll(pairs(5, 1));

        var drills = Drill.drillsOf(segments);

        assertEquals(5, drills.size());
        assertEquals(new Drill(1, List.of(new Turn(1, 1, Optional.of(2)))), drills.get(0));
    }

    @Test
    @DisplayName("末尾が有音で終わる列（有音・無音4組 + 有音）は5つのドリルになり、Drill 5 の Turn 1 は無音なし（[8] のみ）")
    void a_trailing_sound_makes_the_last_turn_without_silence() {
        var segments = new ArrayList<Segment>(pairs(4));
        segments
            .add(new Segment(8, Duration.ofSeconds(8), Duration.ofSeconds(1), Segment.Kind.SOUND));

        var drills = Drill.drillsOf(segments);

        assertEquals(5, drills.size());
        assertEquals(new Drill(5, List.of(new Turn(1, 8, Optional.empty()))), drills.get(4));
    }

    @Test
    @DisplayName("有音・無音が6組（12セグメント）の列は5で割り切れないので、IrregularUnitExceptionになる")
    void pairs_that_do_not_split_into_five_drills_are_rejected() {
        assertThrows(IrregularUnitException.class, () -> Drill.drillsOf(pairs(6)));
    }

    @Test
    @DisplayName("空のセグメント列は、空のドリルリストになる")
    void no_segments_give_no_drills() {
        assertEquals(List.of(), Drill.drillsOf(List.of()));
    }

    /** Sound and silence pairs of one second each, numbered from zero. */
    private static List<Segment> pairs(int count) {
        return pairs(count, 0);
    }

    /** Sound and silence pairs of one second each, numbered from the first index. */
    private static List<Segment> pairs(int count, int firstIndex) {
        var segments = new ArrayList<Segment>();
        for (var i = firstIndex; i < firstIndex + count * 2; i++) {
            var kind = (i - firstIndex) % 2 == 0 ? Segment.Kind.SOUND : Segment.Kind.SILENCE;
            segments.add(new Segment(i, Duration.ofSeconds(i), Duration.ofSeconds(1), kind));
        }
        return segments;
    }
}
