package com.sosuisha.domain.exception;

import org.jspecify.annotations.Nullable;

/**
 * Thrown when a repository cannot read or write its storage. See
 * {@link UnrecoverableException} for the error handling policy.
 */
public class RepositoryException extends UnrecoverableException {
    /**
     * Creates the exception.
     *
     * @param message description of the failure, written for the user
     * @param cause underlying cause of the failure, or null when there is none
     * @throws NullPointerException if message is null
     * @throws IllegalArgumentException if message is blank
     */
    public RepositoryException(String message, @Nullable Throwable cause) {
        super(message, cause);
    }
}
