package com.sosuisha.presentation.screens.unit;

import java.util.Optional;

/**
 * Decides how the turn list scrolls to follow the playing turn. The playing
 * row is left alone while it is among the first rows in view, so the rows
 * before it stay visible; otherwise the list scrolls so that the playing row
 * becomes the last of those first rows.
 */
final class TurnListScroll {
    /** The playing row may be this many rows below the first visible row without scrolling. */
    static final int ROWS_KEPT_ABOVE = 6;

    private TurnListScroll() {}

    /**
     * Returns the index that should become the first visible row, or empty
     * when the list should not scroll.
     *
     * @param playingIndex index of the playing row in the list
     * @param firstVisibleIndex index of the first row in view
     * @return the index to scroll to, or empty when no scroll is needed
     * @throws IllegalArgumentException if an index is negative
     */
    static Optional<Integer> firstIndexToShow(int playingIndex, int firstVisibleIndex) {
        if (playingIndex < 0 || firstVisibleIndex < 0) {
            throw new IllegalArgumentException(
                "indexes must not be negative: " + playingIndex + ", " + firstVisibleIndex
            );
        }
        var inView = playingIndex >= firstVisibleIndex
            && playingIndex <= firstVisibleIndex + ROWS_KEPT_ABOVE;
        return inView ? Optional.empty() : Optional.of(Math.max(0, playingIndex - ROWS_KEPT_ABOVE));
    }
}
