package com.apr.community.post;

import java.time.Instant;

// Keyset position of the last row of a likes-sorted page:
// (effective like count, created_at, id), all descending.
public record PostLikeCursor(long likeCount, Instant createdAt, long id) {
}
