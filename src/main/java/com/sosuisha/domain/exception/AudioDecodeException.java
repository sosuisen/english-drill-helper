package com.sosuisha.domain.exception;

import org.jspecify.annotations.Nullable;

/**
 * Thrown when an audio file cannot be decoded, for example because the file
 * does not exist or its format is not supported.
 */
public class AudioDecodeException extends UnrecoverableException {
    /**
     * Creates the exception.
     *
     * @param message description of the failure, written for the user
     * @param cause underlying cause of the failure, or null when there is none
     * @throws NullPointerException if message is null
     * @throws IllegalArgumentException if message is blank
     */
    public AudioDecodeException(String message, @Nullable Throwable cause) {
        super(message, cause);
    }
}
