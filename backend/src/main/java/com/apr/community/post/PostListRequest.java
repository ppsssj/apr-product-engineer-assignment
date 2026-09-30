package com.apr.community.post;

import com.apr.community.common.pagination.CursorPosition;

// A validated post-list request. `query` is trimmed and null when absent or
// blank. Exactly the cursor matching `sort` may be non-null.
public record PostListRequest(
        int limit,
        PostSort sort,
        String query,
        CursorPosition latestCursor,
        PostLikeCursor likesCursor) {
}
