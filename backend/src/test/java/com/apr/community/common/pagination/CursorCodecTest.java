package com.apr.community.common.pagination;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;

import com.apr.community.common.error.ApiException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class CursorCodecTest {

    private final CursorCodec cursorCodec = new CursorCodec();

    @Test
    void roundTripsTimestampWithNanosecondPrecision() {
        CursorPosition cursor = new CursorPosition(
                Instant.parse("2026-08-30T13:55:54.123456789Z"),
                42L);

        String encoded = cursorCodec.encode(cursor);

        assertThat(cursorCodec.decode(encoded)).isEqualTo(cursor);
        assertThat(encoded).doesNotContain("=");
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "%%%", "YWJj=", "not+url/safe"})
    void rejectsMalformedBase64(String encoded) {
        assertInvalidCursor(encoded);
    }

    @Test
    void rejectsPayloadWithoutDelimiter() {
        assertInvalidCursor(encodePayload("2026-08-30T13:55:54Z"));
    }

    @Test
    void rejectsInvalidTimestamp() {
        assertInvalidCursor(encodePayload("not-an-instant|42"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "-1", "not-an-id"})
    void rejectsInvalidId(String id) {
        assertInvalidCursor(encodePayload("2026-08-30T13:55:54Z|" + id));
    }

    private void assertInvalidCursor(String encoded) {
        assertThatThrownBy(() -> cursorCodec.decode(encoded))
                .isInstanceOf(ApiException.class)
                .extracting(exception -> ((ApiException) exception).code())
                .isEqualTo("INVALID_CURSOR");
    }

    private String encodePayload(String payload) {
        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(payload.getBytes(StandardCharsets.UTF_8));
    }
}
