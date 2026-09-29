package com.apr.community.common.pagination;

import java.nio.charset.StandardCharsets;
import java.time.DateTimeException;
import java.time.Instant;
import java.util.Base64;
import java.util.regex.Pattern;

import com.apr.community.common.error.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class CursorCodec {

    private static final Pattern URL_SAFE_BASE64 = Pattern.compile("[A-Za-z0-9_-]+");

    public String encode(CursorPosition cursor) {
        String payload = cursor.createdAt() + "|" + cursor.id();
        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(payload.getBytes(StandardCharsets.UTF_8));
    }

    public CursorPosition decode(String encodedCursor) {
        try {
            if (encodedCursor == null
                    || !URL_SAFE_BASE64.matcher(encodedCursor).matches()) {
                throw invalidCursor();
            }

            byte[] decoded = Base64.getUrlDecoder().decode(encodedCursor);
            String canonical = Base64.getUrlEncoder().withoutPadding().encodeToString(decoded);
            if (!canonical.equals(encodedCursor)) {
                throw invalidCursor();
            }

            String payload = new String(decoded, StandardCharsets.UTF_8);
            int separator = payload.indexOf('|');
            if (separator <= 0 || separator != payload.lastIndexOf('|')) {
                throw invalidCursor();
            }

            Instant createdAt = Instant.parse(payload.substring(0, separator));
            long id = Long.parseLong(payload.substring(separator + 1));
            if (id <= 0) {
                throw invalidCursor();
            }
            return new CursorPosition(createdAt, id);
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
