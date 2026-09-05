package com.sosuisha.domain.model;

import java.util.List;
import java.util.Objects;

/**
 * A drill of the drill book: a run of turns in which the recording speaks
 * and the learner repeats (see ADR 004). Drill 0 is used only for the title
 * of an introduction unit.
 *
 * @param number position of the drill in its unit, from one; zero for the title
 * @param turns turns of the drill in time order
 */
public record Drill(int number, List<Turn> turns) {
    /**
     * Creates the drill. The list is copied.
     *
     * @throws NullPointerException if turns is null
     * @throws IllegalArgumentException if number is negative or turns is empty
     */
    public Drill {
        Objects.requireNonNull(turns, "turns must not be null");
        if (number < 0) {
            throw new IllegalArgumentException("number must not be negative: " + number);
        }
        if (turns.isEmpty()) { throw new IllegalArgumentException("turns must not be empty"); }
        turns = List.copyOf(turns);
    }
}
