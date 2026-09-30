package com.apr.community.post;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;

import com.apr.community.common.error.ApiException;
import com.apr.community.common.pagination.CursorCodec;
import com.apr.community.common.pagination.CursorPosition;
import org.junit.jupiter.api.Test;

class PostLikeCursorCodecTest {

    private final PostLikeCursorCodec codec = new PostLikeCursorCodec();

    @Test
    void roundTripsLikeCountTimestampAndId() {
        var cursor = new PostLikeCursor(1796L, Instant.parse("2026-08-30T13:55:54.123456Z"), 42L);

        assertThat(codec.decode(codec.encode(cursor))).isEqualTo(cursor);
    }

    @Test
    void rejectsLatestCursorAndLatestRejectsLikesCursor() {
        String latest = new CursorCodec().encode(new CursorPosition(Instant.parse("2026-08-30T13:55:54Z"), 5L));
        String likes = codec.encode(new PostLikeCursor(3L, Instant.parse("2026-08-30T13:55:54Z"), 5L));

        assertInvalid(latest);
        assertThatThrownBy(() -> new CursorCodec().decode(likes))
                .isInstanceOfSatisfying(ApiException.class,
                        exception -> assertThat(exception.code()).isEqualTo("INVALID_CURSOR"));
    }

    @Test
    void rejectsMalformedPayloads() {
        assertInvalid("%%%");
        assertInvalid(encodeRaw("likes|abc|2026-08-30T13:55:54Z|5"));
        assertInvalid(encodeRaw("likes|-1|2026-08-30T13:55:54Z|5"));
        assertInvalid(encodeRaw("likes|3|not-a-time|5"));
        assertInvalid(encodeRaw("likes|3|2026-08-30T13:55:54Z|0"));
        assertInvalid(encodeRaw("other|3|2026-08-30T13:55:54Z|5"));
        assertInvalid(encodeRaw("likes|3|2026-08-30T13:55:54Z|5|extra"));
    }

    private void assertInvalid(String cursor) {
        assertThatThrownBy(() -> codec.decode(cursor))
                .isInstanceOfSatisfying(ApiException.class,
                        exception -> assertThat(exception.code()).isEqualTo("INVALID_CURSOR"));
    }

    private static String encodeRaw(String payload) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(payload.getBytes(StandardCharsets.UTF_8));
    }
}
