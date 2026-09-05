package com.sosuisha.presentation.screens.unit;

import java.util.Optional;

/**
 * Decides how the turn list scrolls to follow the playing turn. The playing
 * row is left alone while it is among the first rows in view, so the rows
 * before it stay visible; otherwise the list scrolls so that the playing row
 * becomes the last of those first rows. The row the learner selected is never
 * scrolled to: the learner is already looking at it.
 */
final class TurnListScroll {
    /** The playing row may be this many rows below the first visible row without scrolling. */
    static final int ROWS_KEPT_ABOVE = 6;

    /** The selected index when no row is selected (as in a JavaFX selection model). */
    static final int NO_SELECTION = -1;

    private TurnListScroll() {}

    /**
     * Returns the index that should become the first visible row, or empty
     * when the list should not scroll.
     *
     * @param playingIndex index of the playing row in the list
     * @param firstVisibleIndex index of the first row in view
     * @param selectedIndex index of the selected row, or {@link #NO_SELECTION}
     * @return the index to scroll to, or empty when no scroll is needed
     * @throws IllegalArgumentException if the playing or the first visible index is negative
     */
    static Optional<Integer> firstIndexToShow(
        int playingIndex, int firstVisibleIndex, int selectedIndex) {
        if (playingIndex < 0 || firstVisibleIndex < 0) {
            throw new IllegalArgumentException(
                "indexes must not be negative: " + playingIndex + ", " + firstVisibleIndex
            );
        }
        if (playingIndex == selectedIndex) { return Optional.empty(); }
        var inView = playingIndex >= firstVisibleIndex
            && playingIndex <= firstVisibleIndex + ROWS_KEPT_ABOVE;
        return inView ? Optional.empty() : Optional.of(Math.max(0, playingIndex - ROWS_KEPT_ABOVE));
    }
}
