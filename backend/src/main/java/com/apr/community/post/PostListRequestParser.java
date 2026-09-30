package com.apr.community.post;

import com.apr.community.common.error.ApiException;
import com.apr.community.common.pagination.CursorCodec;
import com.apr.community.common.pagination.PageRequestParser;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

// Validates `GET /api/posts` parameters: limit (shared 1..50 rule), sort,
// search query, then the cursor in the format of the selected sort.
@Component
public class PostListRequestParser {

    public static final int MAX_QUERY_CODE_POINTS = 100;

    private final PageRequestParser pageRequestParser;
    private final CursorCodec cursorCodec;
    private final PostLikeCursorCodec postLikeCursorCodec;

    public PostListRequestParser(
            PageRequestParser pageRequestParser,
            CursorCodec cursorCodec,
            PostLikeCursorCodec postLikeCursorCodec) {
        this.pageRequestParser = pageRequestParser;
        this.cursorCodec = cursorCodec;
        this.postLikeCursorCodec = postLikeCursorCodec;
    }

    public PostListRequest parse(Integer limit, String cursor, String sort, String query) {
        int validLimit = pageRequestParser.parseLimit(limit);
        PostSort validSort = PostSort.parse(sort);
        String validQuery = normalizeQuery(query);

        if (cursor == null) {
            return new PostListRequest(validLimit, validSort, validQuery, null, null);
        }
        return switch (validSort) {
            case LATEST -> new PostListRequest(
                    validLimit, validSort, validQuery, cursorCodec.decode(cursor), null);
            case LIKES -> new PostListRequest(
                    validLimit, validSort, validQuery, null, postLikeCursorCodec.decode(cursor));
        };
    }

    private String normalizeQuery(String query) {
        if (query == null || query.isBlank()) {
            return null;
        }
        String trimmed = query.strip();
        if (trimmed.codePointCount(0, trimmed.length()) > MAX_QUERY_CODE_POINTS) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "INVALID_QUERY",
                    "Search query must be at most " + MAX_QUERY_CODE_POINTS + " characters.");
        }
        return trimmed;
    }
}
