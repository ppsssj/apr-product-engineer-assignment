package com.apr.community.common.pagination;

import com.apr.community.common.error.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class PageRequestParser {

    public static final int DEFAULT_LIMIT = 20;
    public static final int MAX_LIMIT = 50;

    private final CursorCodec cursorCodec;

    public PageRequestParser(CursorCodec cursorCodec) {
        this.cursorCodec = cursorCodec;
    }

    public PageRequest parse(Integer requestedLimit, String encodedCursor) {
        int limit = requestedLimit == null ? DEFAULT_LIMIT : requestedLimit;
        if (limit < 1 || limit > MAX_LIMIT) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "INVALID_LIMIT",
                    "Limit must be between 1 and 50.");
        }

        CursorPosition cursor = encodedCursor == null ? null : cursorCodec.decode(encodedCursor);
        return new PageRequest(limit, cursor);
    }
}
