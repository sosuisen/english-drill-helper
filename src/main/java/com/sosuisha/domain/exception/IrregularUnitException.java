package com.sosuisha.domain.exception;

import org.jspecify.annotations.Nullable;

/**
 * Thrown when the segments of a unit do not split into the fixed number of
 * drills of the drill book, for example because a pause between sentences
 * was not detected.
 */
public class IrregularUnitException extends UnrecoverableException {
    /**
     * Creates the exception.
     *
     * @param message description of the failure, written for the user
     * @param cause underlying cause of the failure, or null when there is none
     * @throws NullPointerException if message is null
     * @throws IllegalArgumentException if message is blank
     */
    public IrregularUnitException(String message, @Nullable Throwable cause) {
        super(message, cause);
    }
}
