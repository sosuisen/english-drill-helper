package com.sosuisha.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.sosuisha.domain.exception.IrregularUnitException;

class DrillBookTest {
    // セグメント列は有音の長さ（秒）で組み立てる。無音は原則2秒。Key の対は同じ長さ、Cue は短い
    private static final Unit REGULAR_UNIT = unit("011_Unit 1.1.mp3");
    private static final Unit INTRODUCTION_UNIT = unit("001_Unit 0.1.mp3");
    private static final double KEY = 3.0;
    private static final double CUE = 0.6;
    private static final double ANSWER = 2.5;

    @Test
    @DisplayName("通常ユニット: 「Key・Key・Cue・Answer・Answer」× 5 の列は、番号1〜5の5ドリルになり、各ターンの役割が Key・Key・Cue・Answer・Answer である")
    void five_drills_of_a_key_pair_and_one_set_each() {
        var sounds = new ArrayList<Double>();
        for (var d = 0; d < 5; d++) {
            sounds.addAll(List.of(KEY + d, KEY + d, CUE, ANSWER + d, ANSWER + d));
        }

        var drills = DrillBook.drillsOf(sounds(sounds), REGULAR_UNIT);

        assertEquals(List.of(1, 2, 3, 4, 5), drills.stream().map(Drill::number).toList());
        assertEquals(
            List.of(
                Turn.Role.KEY_SENTENCE, Turn.Role.KEY_SENTENCE, Turn.Role.CUE, Turn.Role.ANSWER,
                Turn.Role.ANSWER
            ),
            drills.get(0).turns().stream().map(Turn::role).toList()
        );
        assertEquals(
            List.of(1, 2, 3, 4, 5), drills.get(0).turns().stream().map(Turn::number).toList()
        );
        assertEquals(List.of(0, 1), drills.get(0).turns().get(0).segmentIndexes());
        assertEquals(List.of(40, 41), drills.get(4).turns().get(0).segmentIndexes());
    }

    @Test
    @DisplayName("通常ユニット: 組の数がドリルごとに 2・1・2・2・1 の列は、Key の対を境界として 8・5・8・8・5 ターンの5ドリルになる")
    void drills_with_different_numbers_of_sets_are_split_at_the_key_pairs() {
        var sounds = new ArrayList<Double>();
        var setsPerDrill = List.of(2, 1, 2, 2, 1);
        for (var d = 0; d < 5; d++) {
            sounds.addAll(List.of(KEY + d, KEY + d));
            for (var s = 0; s < setsPerDrill.get(d); s++) {
                sounds.addAll(List.of(CUE, ANSWER + d + s * 0.5, ANSWER + d + s * 0.5));
            }
        }

        var drills = DrillBook.drillsOf(sounds(sounds), REGULAR_UNIT);

        assertEquals(
            List.of(8, 5, 8, 8, 5), drills.stream().map(drill -> drill.turns().size()).toList()
        );
    }

    @Test
    @DisplayName("通常ユニット: 冒頭の Key の対の長さが合わなくても（タイトルが合体した場合）、冒頭の2ターンは Key の対になる")
    void the_first_two_turns_are_key_sentences_even_when_their_lengths_differ() {
        var sounds = new ArrayList<Double>(List.of(3.9, 3.2, CUE, ANSWER, ANSWER));
        for (var d = 1; d < 5; d++) {
            sounds.addAll(List.of(KEY + d, KEY + d, CUE, ANSWER + d, ANSWER + d));
        }

        var drills = DrillBook.drillsOf(sounds(sounds), REGULAR_UNIT);

        assertEquals(5, drills.size());
        assertEquals(Turn.Role.KEY_SENTENCE, drills.get(0).turns().get(0).role());
        assertEquals(Turn.Role.KEY_SENTENCE, drills.get(0).turns().get(1).role());
    }

    @Test
    @DisplayName("通常ユニット: Key sentence がポーズで「a・b・a・b」に割れていても、(a+b) の長さが等しければ、各 Key は有音・ポーズ・有音・無音の4セグメントを持つ1ターンに結合される")
    void a_key_sentence_split_by_a_pause_is_joined_back_into_one_turn() {
        var parts = new ArrayList<Part>();
        for (var k = 0; k < 2; k++) {
            parts.addAll(List.of(sound(1.2), silence(1.1), sound(2.4), silence(2.0)));
        }
        parts.addAll(set(ANSWER));
        for (var d = 1; d < 5; d++) {
            parts.addAll(keyPair(KEY + d));
            parts.addAll(set(ANSWER + d));
        }

        var drills = DrillBook.drillsOf(layout(parts), REGULAR_UNIT);

        assertEquals(5, drills.size());
        assertEquals(List.of(0, 1, 2, 3), drills.get(0).turns().get(0).segmentIndexes());
        assertEquals(List.of(4, 5, 6, 7), drills.get(0).turns().get(1).segmentIndexes());
        assertEquals(Turn.Role.CUE, drills.get(0).turns().get(2).role());
    }

    @Test
    @DisplayName("Unit 1.1 型: Key の対がユニット冒頭に1組だけで、その後に (Cue・Answer・Answer) が5組の列は、Key の対がドリル0になり、各組がドリル1〜5になる")
    void a_unit_with_one_key_pair_has_the_pair_as_drill_zero_and_one_drill_per_set() {
        var sounds = new ArrayList<Double>(List.of(KEY, KEY));
        for (var s = 0; s < 5; s++) {
            sounds.addAll(List.of(CUE, ANSWER + s, ANSWER + s));
        }

        var drills = DrillBook.drillsOf(sounds(sounds), REGULAR_UNIT);

        assertEquals(List.of(0, 1, 2, 3, 4, 5), drills.stream().map(Drill::number).toList());
        assertEquals(
            List.of(2, 3, 3, 3, 3, 3), drills.stream().map(drill -> drill.turns().size()).toList()
        );
        assertEquals(
            List.of(Turn.Role.KEY_SENTENCE, Turn.Role.KEY_SENTENCE),
            drills.get(0).turns().stream().map(Turn::role).toList()
        );
        assertEquals(
            List.of(Turn.Role.CUE, Turn.Role.ANSWER, Turn.Role.ANSWER),
            drills.get(1).turns().stream().map(Turn::role).toList()
        );
    }

    @Test
    @DisplayName("通常ユニット: 解析結果のドリル数が5でない列（Key の対が4組）は、IrregularUnitException になる")
    void a_regular_unit_that_does_not_split_into_five_drills_is_rejected() {
        var sounds = new ArrayList<Double>();
        for (var d = 0; d < 4; d++) {
            sounds.addAll(List.of(KEY + d, KEY + d, CUE, ANSWER + d, ANSWER + d));
        }

        assertThrows(
            IrregularUnitException.class, () -> DrillBook.drillsOf(sounds(sounds), REGULAR_UNIT)
        );
    }

    @Test
    @DisplayName("末尾が有音で終わる列は、最後のターンが無音を持たない（インデックスのリストが有音だけ）")
    void the_last_turn_of_a_file_that_ends_with_sound_has_no_silence() {
        var parts = new ArrayList<Part>();
        for (var d = 0; d < 5; d++) {
            parts.addAll(keyPair(KEY + d));
            parts.addAll(set(ANSWER + d));
        }
        parts.removeLast(); // the silence after the last answer

        var drills = DrillBook.drillsOf(layout(parts), REGULAR_UNIT);

        var last = drills.get(4).turns().getLast();
        assertEquals(1, last.segmentIndexes().size());
    }

    @Test
    @DisplayName("Unit 0.x: 「タイトル → (Cue → 文 × 4) × 2」の列は、ドリル0（タイトル、役割 SENTENCE）と、Cue で始まる2つのドリル（役割 CUE・SENTENCE × 4）になる")
    void an_introduction_unit_has_the_title_as_drill_zero_and_a_drill_per_cue() {
        var sounds = List.of(1.5, 0.4, 1.4, 1.4, 1.3, 1.3, 0.35, 1.6, 1.6, 1.4, 1.4);

        var drills = DrillBook.drillsOf(sounds(sounds), INTRODUCTION_UNIT);

        assertEquals(List.of(0, 1, 2), drills.stream().map(Drill::number).toList());
        assertEquals(
            List.of(Turn.Role.SENTENCE), drills.get(0).turns().stream().map(Turn::role).toList()
        );
        assertEquals(
            List.of(
                Turn.Role.CUE, Turn.Role.SENTENCE, Turn.Role.SENTENCE, Turn.Role.SENTENCE,
                Turn.Role.SENTENCE
            ),
            drills.get(1).turns().stream().map(Turn::role).toList()
        );
        assertEquals(List.of(2, 3), drills.get(1).turns().get(0).segmentIndexes());
    }

    @Test
    @DisplayName("Unit 0.x: 文が短くても（0.72秒の対）、等しい長さの対は文であり Cue ではない。Cue は対に属さない有音（Unit 0.5）")
    void short_sentence_pairs_of_an_introduction_unit_are_sentences_not_cues() {
        var sounds = List.of(1.87, 0.46, 1.00, 1.00, 0.72, 0.72, 0.44, 1.66, 1.66, 1.25, 1.25);

        var drills = DrillBook.drillsOf(sounds(sounds), INTRODUCTION_UNIT);

        assertEquals(List.of(0, 1, 2), drills.stream().map(Drill::number).toList());
        assertEquals(
            List.of(
                Turn.Role.CUE, Turn.Role.SENTENCE, Turn.Role.SENTENCE, Turn.Role.SENTENCE,
                Turn.Role.SENTENCE
            ),
            drills.get(1).turns().stream().map(Turn::role).toList()
        );
    }

    @Test
    @DisplayName("Unit 0.x: ドリル数は検証しない。Cue が3つでも例外にならず3ドリルになる")
    void an_introduction_unit_may_have_any_number_of_drills() {
        var sounds = List.of(1.5, 0.4, 1.4, 1.4, 0.4, 1.4, 1.4, 0.4, 1.4, 1.4);

        var drills = DrillBook.drillsOf(sounds(sounds), INTRODUCTION_UNIT);

        assertEquals(List.of(0, 1, 2, 3), drills.stream().map(Drill::number).toList());
    }

    @Test
    @DisplayName("空のセグメント列は、空のドリルリストになる")
    void no_segments_give_no_drills() {
        assertEquals(List.of(), DrillBook.drillsOf(List.of(), REGULAR_UNIT));
    }

    @Test
    @DisplayName("通常ユニット: Key の対や Answer の対の長さの差が0.35秒以内なら同じ文とみなす（natural 版のばらつき。Unit 9.2 natural の 1.92秒と2.24秒）")
    void sentences_that_differ_by_up_to_a_third_of_a_second_are_the_same_sentence() {
        var sounds = new ArrayList<Double>(List.of(KEY, KEY, CUE, ANSWER, ANSWER));
        sounds.addAll(List.of(1.92, 2.24, CUE, 1.84, 1.58));
        for (var d = 2; d < 5; d++) {
            sounds.addAll(List.of(KEY + d, KEY + d, CUE, ANSWER + d, ANSWER + d));
        }

        var drills = DrillBook.drillsOf(sounds(sounds), REGULAR_UNIT);

        assertEquals(5, drills.size());
        assertEquals(Turn.Role.KEY_SENTENCE, drills.get(1).turns().get(1).role());
    }

    @Test
    @DisplayName("通常ユニット: 冒頭の2ターンの長さが合わなくても、間の無音が長ければ（ポーズではない）結合しない。合計が偶然一致しても Key の対はそのまま（Unit 2.4 natural）")
    void turns_separated_by_a_long_silence_are_not_joined_even_when_the_sums_agree() {
        var parts = new ArrayList<Part>();
        parts.addAll(List.of(sound(2.36), silence(2.16), sound(1.70), silence(2.18)));
        parts.addAll(
            List.of(
                sound(1.46), silence(3.06), sound(2.52), silence(3.0), sound(2.54), silence(3.0)
            )
        );
        for (var s = 1; s < 5; s++) {
            parts.addAll(set(2.0 + s * 0.3));
        }

        var drills = DrillBook.drillsOf(layout(parts), REGULAR_UNIT);

        assertEquals(
            List.of(2, 3, 3, 3, 3, 3), drills.stream().map(drill -> drill.turns().size()).toList()
        );
        assertEquals(List.of(0, 1), drills.get(0).turns().get(0).segmentIndexes());
        assertEquals(List.of(2, 3), drills.get(0).turns().get(1).segmentIndexes());
    }

    @Test
    @DisplayName("通常ユニット: 途中のドリルで片方の Key だけがポーズで割れていても、結合した長さ（有音・ポーズ・有音）が相手の Key と等しければ Key の対になる（Unit 17.2 slow）")
    void a_key_sentence_split_by_a_pause_in_the_middle_of_the_unit_is_joined_to_match_its_partner() {
        var parts = new ArrayList<Part>();
        for (var d = 0; d < 3; d++) {
            parts.addAll(keyPair(KEY + d));
            parts.addAll(set(ANSWER + d));
        }
        parts.addAll(
            List.of(
                sound(4.44), silence(5.06), sound(1.68), silence(1.0), sound(1.74), silence(5.08)
            )
        );
        parts.addAll(set(3.46));
        parts.addAll(keyPair(KEY + 4));
        parts.addAll(set(ANSWER + 4));

        var drills = DrillBook.drillsOf(layout(parts), REGULAR_UNIT);

        assertEquals(5, drills.size());
        var fourth = drills.get(3).turns();
        assertEquals(Turn.Role.KEY_SENTENCE, fourth.get(1).role());
        assertEquals(4, fourth.get(1).segmentIndexes().size());
        assertEquals(Turn.Role.CUE, fourth.get(2).role());
    }

    @Test
    @DisplayName("通常ユニット: 冒頭の Key がタイトルと合体し、かつポーズで割れているとき、両方にポーズがあり後半の長さが等しければ結合する（Unit 14.5 natural）")
    void a_split_first_key_sentence_with_the_title_merged_is_joined_when_the_second_halves_agree() {
        var parts = new ArrayList<Part>();
        parts.addAll(List.of(sound(1.88), silence(1.10), sound(1.96), silence(4.92)));
        parts.addAll(List.of(sound(1.20), silence(1.10), sound(1.96), silence(4.92)));
        parts.addAll(set(3.0));
        for (var d = 1; d < 5; d++) {
            parts.addAll(keyPair(KEY + d));
            parts.addAll(set(ANSWER + d));
        }

        var drills = DrillBook.drillsOf(layout(parts), REGULAR_UNIT);

        assertEquals(5, drills.size());
        assertEquals(List.of(0, 1, 2, 3), drills.get(0).turns().get(0).segmentIndexes());
        assertEquals(List.of(4, 5, 6, 7), drills.get(0).turns().get(1).segmentIndexes());
        assertEquals(Turn.Role.CUE, drills.get(0).turns().get(2).role());
    }

    @Test
    @DisplayName("通常ユニット: 長さの一致は長い方の20%以内（下限0.1秒）で判定する。短い文では 0.38秒と0.60秒は別の文であり、Cue と Answer の組として扱う（Unit 6.2 natural）")
    void short_sounds_that_differ_by_more_than_a_fifth_are_not_the_same_sentence() {
        var parts = new ArrayList<Part>();
        parts.addAll(keyPair(1.68));
        parts.addAll(
            List.of(
                sound(0.54), silence(1.70), sound(1.10), silence(1.68), sound(1.08), silence(1.68)
            )
        );
        parts.addAll(
            List.of(
                sound(0.38), silence(1.22), sound(0.60), silence(1.22), sound(0.60), silence(1.24)
            )
        );
        for (var d = 1; d < 5; d++) {
            parts.addAll(keyPair(KEY + d));
            parts.addAll(set(ANSWER + d));
        }

        var drills = DrillBook.drillsOf(layout(parts), REGULAR_UNIT);

        assertEquals(5, drills.size());
        assertEquals(8, drills.get(0).turns().size());
        assertEquals(Turn.Role.CUE, drills.get(0).turns().get(5).role());
    }

    @Test
    @DisplayName("通常ユニット: Key の対の候補は、その後に「Cue → 等しい Answer × 2」の組が続くときだけ Key の対とする。最後の組の Cue と Answer の長さが近くても Key の対にはならない（Unit 2.1 natural）")
    void a_key_pair_candidate_without_a_set_after_it_is_a_cue_and_an_answer() {
        var parts = new ArrayList<Part>();
        parts.addAll(List.of(sound(2.18), silence(2.12), sound(1.50), silence(2.12)));
        parts.addAll(set(1.46));
        parts.addAll(set(1.10));
        parts.addAll(set(1.06));
        parts.addAll(set(0.96));
        parts.addAll(
            List.of(
                sound(1.18), silence(2.04), sound(1.30), silence(2.12), sound(1.30), silence(4.51)
            )
        );

        var drills = DrillBook.drillsOf(layout(parts), REGULAR_UNIT);

        assertEquals(
            List.of(2, 3, 3, 3, 3, 3), drills.stream().map(drill -> drill.turns().size()).toList()
        );
        assertEquals(Turn.Role.CUE, drills.get(5).turns().get(0).role());
    }

    // --- builders ---

    private record Part(Segment.Kind kind, double seconds) {
    }

    private static Part sound(double seconds) {
        return new Part(Segment.Kind.SOUND, seconds);
    }

    private static Part silence(double seconds) {
        return new Part(Segment.Kind.SILENCE, seconds);
    }

    /** A key sentence spoken twice, each followed by two seconds of silence. */
    private static List<Part> keyPair(double seconds) {
        return List.of(sound(seconds), silence(2.0), sound(seconds), silence(2.0));
    }

    /** A cue and its answer spoken twice, each followed by two seconds of silence. */
    private static List<Part> set(double answerSeconds) {
        return List.of(
            sound(CUE), silence(2.0), sound(answerSeconds), silence(2.0), sound(answerSeconds),
            silence(2.0)
        );
    }

    /** Sounds of the given lengths, each followed by two seconds of silence. */
    private static List<Segment> sounds(List<Double> soundSeconds) {
        var parts = new ArrayList<Part>();
        for (var seconds : soundSeconds) {
            parts.add(sound(seconds));
            parts.add(silence(2.0));
        }
        return layout(parts);
    }

    /** Segments of the parts in time order, numbered from zero. */
    private static List<Segment> layout(List<Part> parts) {
        var segments = new ArrayList<Segment>();
        var start = Duration.ZERO;
        for (var part : parts) {
            var duration = Duration.ofMillis(Math.round(part.seconds() * 1000));
            segments.add(new Segment(segments.size(), start, duration, part.kind()));
            start = start.plus(duration);
        }
        return segments;
    }

    private static Unit unit(String fileName) {
        return new Unit(
            new AudioFile(Path.of("units", fileName), "fingerprint-of-" + fileName),
            Optional.empty()
        );
    }
}
