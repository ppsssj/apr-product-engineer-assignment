package com.apr.community.post;

import com.apr.community.common.error.ApiException;
import org.springframework.http.HttpStatus;

// Post-list ordering. LATEST is the default so `GET /api/posts` without `sort`
// keeps its original newest-first contract.
public enum PostSort {
    LATEST,
    LIKES;

    public static PostSort parse(String value) {
        if (value == null) {
            return LATEST;
        }
        return switch (value) {
            case "latest" -> LATEST;
            case "likes" -> LIKES;
            default -> throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "INVALID_SORT",
                    "Sort must be latest or likes.");
        };
    }
}
