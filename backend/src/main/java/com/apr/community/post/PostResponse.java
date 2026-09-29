package com.apr.community.post;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonFormat;

public record PostResponse(
        long id,
        String title,
        String content,
        long likeCount,
        long commentCount,
        boolean isLiked,
        String author,
        @JsonFormat(shape = JsonFormat.Shape.STRING)
        Instant createdAt) {
}
