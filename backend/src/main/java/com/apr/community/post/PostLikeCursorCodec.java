package com.apr.community.post;

import java.nio.charset.StandardCharsets;
import java.time.DateTimeException;
import java.time.Instant;
import java.util.Base64;
import java.util.regex.Pattern;

import com.apr.community.common.error.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

// Opaque cursor for likes-sorted post pages. The payload is tagged
// ("likes|count|createdAt|id") so a latest cursor ("createdAt|id", see
// CursorCodec) can never be read as a likes cursor, and vice versa.
@Component
public class PostLikeCursorCodec {

    private static final String TAG = "likes";
    private static final Pattern URL_SAFE_BASE64 = Pattern.compile("[A-Za-z0-9_-]+");

    public String encode(PostLikeCursor cursor) {
        String payload = TAG + "|" + cursor.likeCount() + "|" + cursor.createdAt() + "|" + cursor.id();
        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(payload.getBytes(StandardCharsets.UTF_8));
    }

    public PostLikeCursor decode(String encodedCursor) {
        try {
            if (encodedCursor == null || !URL_SAFE_BASE64.matcher(encodedCursor).matches()) {
                throw invalidCursor();
            }

            byte[] decoded = Base64.getUrlDecoder().decode(encodedCursor);
            String canonical = Base64.getUrlEncoder().withoutPadding().encodeToString(decoded);
            if (!canonical.equals(encodedCursor)) {
                throw invalidCursor();
            }

            String[] parts = new String(decoded, StandardCharsets.UTF_8).split("\\|", -1);
            if (parts.length != 4 || !TAG.equals(parts[0])) {
                throw invalidCursor();
            }

            long likeCount = Long.parseLong(parts[1]);
            Instant createdAt = Instant.parse(parts[2]);
            long id = Long.parseLong(parts[3]);
            if (likeCount < 0 || id <= 0) {
                throw invalidCursor();
            }
            return new PostLikeCursor(likeCount, createdAt, id);
        } catch (ApiException exception) {
            throw exception;
        } catch (DateTimeException | IllegalArgumentException exception) {
            throw invalidCursor();
        }
    }

    private ApiException invalidCursor() {
        return new ApiException(HttpStatus.BAD_REQUEST, "INVALID_CURSOR", "Cursor is malformed.");
    }
}
