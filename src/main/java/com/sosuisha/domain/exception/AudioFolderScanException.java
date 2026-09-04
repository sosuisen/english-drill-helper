package com.sosuisha.domain.exception;

import org.jspecify.annotations.Nullable;

/**
 * Thrown when the audio folder cannot be scanned, for example because the
 * folder does not exist.
 */
public class AudioFolderScanException extends UnrecoverableException {
    /**
     * Creates the exception.
     *
     * @param message description of the failure, written for the user
     * @param cause underlying cause of the failure, or null when there is none
     * @throws NullPointerException if message is null
     * @throws IllegalArgumentException if message is blank
     */
    public AudioFolderScanException(String message, @Nullable Throwable cause) {
        super(message, cause);
    }
}
