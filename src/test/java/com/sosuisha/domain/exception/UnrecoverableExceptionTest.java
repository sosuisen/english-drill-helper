package com.sosuisha.domain.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.jspecify.annotations.NullUnmarked;

// These tests pass null on purpose to check the runtime contract.
@NullUnmarked
class UnrecoverableExceptionTest {
    @Test
    @DisplayName("UnrecoverableExceptionは、非チェック例外であり、人間向けのメッセージを持っている必要がある")
    void unrecoverable_exception_is_unchecked_and_must_have_a_message_for_the_user() {
        var exception = new UnrecoverableException("message", null) {};

        assertInstanceOf(RuntimeException.class, exception);
        assertEquals("message", exception.getMessage());
        assertThrows(NullPointerException.class, () -> new UnrecoverableException(null, null) {});
        assertThrows(
            IllegalArgumentException.class, () -> new UnrecoverableException(" ", null) {}
        );
    }
}
