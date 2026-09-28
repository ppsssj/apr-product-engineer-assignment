package com.apr.community.seed;

import java.time.Instant;
import java.util.List;

public record SeedData(
        List<SeedPost> posts,
        List<SeedComment> comments,
        List<Long> deletedPostIds,
        List<Long> deletedCommentIds) {

    public record SeedPost(
            long id,
            String title,
            String content,
            int likeCount,
            String author,
            Instant createdAt) {
    }

    public record SeedComment(
            long id,
            long postId,
            String content,
            String author,
            Instant createdAt) {
    }
}
