package com.apr.community.comment;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonFormat;

public record CommentResponse(
        long id,
        long postId,
        String content,
        String author,
        @JsonFormat(shape = JsonFormat.Shape.STRING)
        Instant createdAt) {
}
