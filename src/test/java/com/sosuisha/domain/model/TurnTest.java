package com.sosuisha.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TurnTest {
    @Test
    @DisplayName("ターンは、ドリル内の番号と、役割と、含まれるセグメントのインデックスのリストを持つ")
    void turn_has_a_number_a_role_and_the_indexes_of_its_segments() {
        var turn = new Turn(2, Turn.Role.KEY_SENTENCE, List.of(4, 5, 6, 7));

        assertEquals(2, turn.number());
        assertEquals(Turn.Role.KEY_SENTENCE, turn.role());
        assertEquals(List.of(4, 5, 6, 7), turn.segmentIndexes());
    }

    @Test
    @DisplayName("番号が1未満のターンや、セグメントを1つも含まないターンは作れない")
    void turn_with_a_number_below_one_or_without_segments_cannot_be_created() {
        assertThrows(
            IllegalArgumentException.class, () -> new Turn(0, Turn.Role.CUE, List.of(0, 1))
        );
        assertThrows(
            IllegalArgumentException.class, () -> new Turn(1, Turn.Role.CUE, List.of())
        );
    }
}
