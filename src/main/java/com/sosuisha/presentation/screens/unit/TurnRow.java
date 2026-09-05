package com.sosuisha.presentation.screens.unit;

import java.util.Objects;

import com.sosuisha.domain.model.Turn;

/**
 * A row of the turn list on the unit screen: one turn with the number of its
 * drill, shown as "drill-turn" such as {@code 1-2}.
 *
 * @param drillNumber number of the drill the turn belongs to, from one
 * @param turn the turn
 */
record TurnRow(int drillNumber, Turn turn) {
    /**
     * Creates the row.
     *
     * @throws NullPointerException if turn is null
     * @throws IllegalArgumentException if drillNumber is below one
     */
    TurnRow {
        Objects.requireNonNull(turn, "turn must not be null");
        if (drillNumber < 1) {
            throw new IllegalArgumentException("drillNumber must be one or more: " + drillNumber);
        }
    }

    /**
     * Returns the text shown in the list.
     *
     * @return the drill number and the turn number joined by a hyphen, such as {@code 1-2}
     */
    String label() {
        return drillNumber + "-" + turn.number();
    }
}
