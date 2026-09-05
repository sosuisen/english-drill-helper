package com.sosuisha.presentation.screens.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TurnListScrollTest {
    @Test
    @DisplayName("再生中の行が可視範囲の先頭から7行目以内にあれば、スクロールしない")
    void no_scroll_while_the_playing_row_is_within_the_first_seven_visible_rows() {
        assertEquals(
            Optional.empty(), TurnListScroll.firstIndexToShow(10, 10, TurnListScroll.NO_SELECTION)
        );
        assertEquals(
            Optional.empty(), TurnListScroll.firstIndexToShow(16, 10, TurnListScroll.NO_SELECTION)
        );
    }

    @Test
    @DisplayName("再生中の行が可視範囲の8行目以降にあれば、その行が7行目に来る位置（6行上を残す）までスクロールする")
    void scroll_so_that_the_playing_row_becomes_the_seventh_visible_row_when_it_is_below() {
        assertEquals(
            Optional.of(11), TurnListScroll.firstIndexToShow(17, 10, TurnListScroll.NO_SELECTION)
        );
        assertEquals(
            Optional.of(18), TurnListScroll.firstIndexToShow(24, 10, TurnListScroll.NO_SELECTION)
        );
    }

    @Test
    @DisplayName("再生中の行が可視範囲より上にあれば、その行が7行目に来る位置までスクロールする。先頭付近では0行目まで")
    void scroll_up_to_the_playing_row_keeping_six_rows_above_it_but_not_before_the_first_row() {
        assertEquals(
            Optional.of(3), TurnListScroll.firstIndexToShow(9, 10, TurnListScroll.NO_SELECTION)
        );
        assertEquals(
            Optional.of(0), TurnListScroll.firstIndexToShow(2, 10, TurnListScroll.NO_SELECTION)
        );
    }

    @Test
    @DisplayName("再生中の行がクリックで選択した行そのものなら、可視範囲の外にあってもスクロールしない。選択した行は学習者が見ているため")
    void no_scroll_when_the_playing_row_is_the_selected_row() {
        assertEquals(Optional.empty(), TurnListScroll.firstIndexToShow(24, 10, 24));
        assertEquals(Optional.empty(), TurnListScroll.firstIndexToShow(2, 10, 2));
    }

    @Test
    @DisplayName("再生中の行が選択した行と違えば、選択の有無にかかわらずこれまでの規則で決まる（8行目以降なら6行上を残す位置へ）")
    void the_usual_rule_applies_when_the_playing_row_differs_from_the_selected_row() {
        assertEquals(Optional.of(18), TurnListScroll.firstIndexToShow(24, 10, 0));
        assertEquals(
            Optional.of(18), TurnListScroll.firstIndexToShow(24, 10, TurnListScroll.NO_SELECTION)
        );
    }


}
