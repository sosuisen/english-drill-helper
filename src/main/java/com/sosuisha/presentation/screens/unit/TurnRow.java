package com.sosuisha.presentation.screens.unit;

import java.util.Objects;

import com.sosuisha.domain.model.Turn;

/**
 * A row of the turn list on the unit screen: one turn with the number of its
 * drill, shown as "drill-turn" such as {@code 1-2}, with {@code [Key]} added
 * for a key sentence.
 *
 * @param drillNumber number of the drill the turn belongs to, zero or more
 * @param turn the turn
 */
record TurnRow(int drillNumber, Turn turn) {
    private static final String KEY_SENTENCE_MARK = " [Key]";

    /**
     * Creates the row.
     *
     * @throws NullPointerException if turn is null
     * @throws IllegalArgumentException if drillNumber is negative
     */
    TurnRow {
        Objects.requireNonNull(turn, "turn must not be null");
        if (drillNumber < 0) {
            throw new IllegalArgumentException("drillNumber must not be negative: " + drillNumber);
        }
    }

    /**
     * Returns the text shown in the list.
     *
     * @return the drill number and the turn number joined by a hyphen, such as
     *         {@code 1-2}, followed by {@code [Key]} for a key sentence
     */
    String label() {
        var label = drillNumber + "-" + turn.number();
        return turn.role() == Turn.Role.KEY_SENTENCE ? label + KEY_SENTENCE_MARK : label;
    }
}
