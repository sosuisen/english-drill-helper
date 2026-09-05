package com.sosuisha.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class DrillTest {
    private static final List<Turn> TURNS = List.of(
        new Turn(1, Turn.Role.KEY_SENTENCE, List.of(0, 1)),
        new Turn(2, Turn.Role.KEY_SENTENCE, List.of(2, 3))
    );

    @Test
    @DisplayName("ドリルは、ユニット内の番号とターンのリストを持つ")
    void drill_holds_a_number_and_its_turns() {
        var drill = new Drill(1, TURNS);

        assertEquals(1, drill.number());
        assertEquals(TURNS, drill.turns());
    }

    @Test
    @DisplayName("ドリルの番号は0を許す（導入ユニットのタイトル用）")
    void drill_number_zero_is_allowed_for_the_title_of_an_introduction_unit() {
        assertEquals(0, new Drill(0, TURNS).number());
    }

    @Test
    @DisplayName("番号が負のドリルや、ターンを1つも含まないドリルは作れない")
    void drill_with_a_negative_number_or_without_turns_cannot_be_created() {
        assertThrows(IllegalArgumentException.class, () -> new Drill(-1, TURNS));
        assertThrows(IllegalArgumentException.class, () -> new Drill(1, List.of()));
    }
}
