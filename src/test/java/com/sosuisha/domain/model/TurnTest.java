package com.sosuisha.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TurnTest {
    @Test
    @DisplayName("ターンは、ドリル内の番号と、有音セグメントのインデックスと、あれば無音セグメントのインデックスを持つ")
    void turn_has_a_number_a_sound_index_and_an_optional_silence_index() {
        var turn = new Turn(2, 2, Optional.of(3));
        var last = new Turn(3, 4, Optional.empty());

        assertEquals(2, turn.number());
        assertEquals(2, turn.soundIndex());
        assertEquals(Optional.of(3), turn.silenceIndex());
        assertEquals(Optional.empty(), last.silenceIndex());
    }

    @Test
    @DisplayName("番号が1未満のターンは作れない")
    void turn_with_a_number_below_one_cannot_be_created() {
        assertThrows(IllegalArgumentException.class, () -> new Turn(0, 0, Optional.of(1)));
    }
}
