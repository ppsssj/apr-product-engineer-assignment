package com.apr.community.common.validation;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.apr.community.common.error.ApiException;
import org.junit.jupiter.api.Test;

class MutationValidatorTest {

    private final MutationValidator validator = new MutationValidator();

    @Test
    void acceptsTwentyUnicodeCodePointsEvenWhenTheyUseSurrogatePairs() {
        assertDoesNotThrow(() -> validator.validatePost("a".repeat(20), "content"));
        assertDoesNotThrow(() -> validator.validatePost("😀".repeat(20), "content"));
    }

    @Test
    void rejectsTitleOverTwentyUnicodeCodePoints() {
        ApiException exception = assertThrows(
                ApiException.class,
                () -> validator.validatePost("😀".repeat(21), "content"));

        assertEquals("INVALID_TITLE", exception.code());
        assertEquals(400, exception.status().value());
    }

    @Test
    void rejectsNullAndBlankPostFields() {
        assertEquals("INVALID_TITLE", assertThrows(
                ApiException.class,
                () -> validator.validatePost(null, "content")).code());
        assertEquals("INVALID_TITLE", assertThrows(
                ApiException.class,
                () -> validator.validatePost(" \t", "content")).code());
        assertEquals("INVALID_CONTENT", assertThrows(
                ApiException.class,
                () -> validator.validatePost("title", "\n ")).code());
    }

    @Test
    void rejectsBlankCommentButDoesNotImposeAMaximumLength() {
        assertEquals("INVALID_CONTENT", assertThrows(
                ApiException.class,
                () -> validator.validateComment(null)).code());
        assertEquals("INVALID_CONTENT", assertThrows(
                ApiException.class,
                () -> validator.validateComment("  ")).code());
        assertDoesNotThrow(() -> validator.validateComment("c".repeat(10_000)));
    }
}
