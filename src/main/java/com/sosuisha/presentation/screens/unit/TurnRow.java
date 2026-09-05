package com.sosuisha.presentation.screens.unit;

import java.util.Objects;

import com.sosuisha.domain.model.Turn;

/**
 * A row of the turn list on the unit screen: one turn with the number of its
 * drill, shown as "drill-turn" such as {@code 1-2}. The shown number counts
 * only the turns the learner repeats, so a cue takes no number and has no
 * label. The role of a turn is not part of the label; the list shows it with
 * an icon.
 *
 * @param drillNumber number of the drill the turn belongs to, zero or more
 * @param turn the turn
 * @param shownNumber number shown for the turn, counted without the cues of
 *        the drill; zero for a cue
 */
record TurnRow(int drillNumber, Turn turn, int shownNumber) {
    /**
     * Creates the row.
     *
     * @throws NullPointerException if turn is null
     * @throws IllegalArgumentException if drillNumber is negative, if shownNumber
     *             is below one for a turn that is not a cue, or if shownNumber is
     *             not zero for a cue
     */
    TurnRow {
        Objects.requireNonNull(turn, "turn must not be null");
        if (drillNumber < 0) {
            throw new IllegalArgumentException("drillNumber must not be negative: " + drillNumber);
        }
        var cue = turn.role() == Turn.Role.CUE; // the fields are not set yet here
        if (cue ? shownNumber != 0 : shownNumber < 1) {
            throw new IllegalArgumentException(
                "shownNumber must be zero for a cue and one or more otherwise: " + shownNumber
            );
        }
    }

    /**
     * Tells whether the turn is a cue.
     *
     * @return true if the turn is a cue
     */
    boolean isCue() {
        return turn.role() == Turn.Role.CUE;
    }

    /**
     * Tells whether the turn is a key sentence.
     *
     * @return true if the turn is a key sentence
     */
    boolean isKeySentence() {
        return turn.role() == Turn.Role.KEY_SENTENCE;
    }

    /**
     * Tells whether the turn is the first of its drill.
     *
     * @return true if the turn starts its drill
     */
    boolean startsDrill() {
        return turn.number() == 1;
    }

    /**
     * Returns the text shown in the list.
     *
     * @return the drill number and the shown number joined by a hyphen, such
     *         as {@code 1-2}; empty for a cue
     */
    String label() {
        if (isCue()) { return ""; }
        return drillNumber + "-" + shownNumber;
    }
}
