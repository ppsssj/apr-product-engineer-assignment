package com.apr.community.common.validation;

import com.apr.community.common.error.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class MutationValidator {

    private static final int MAX_TITLE_CODE_POINTS = 20;

    public void validatePost(String title, String content) {
        if (title == null || title.isBlank()) {
            throw badRequest("INVALID_TITLE", "Title is required.");
        }
        if (title.codePointCount(0, title.length()) > MAX_TITLE_CODE_POINTS) {
            throw badRequest("INVALID_TITLE", "Title must contain at most 20 characters.");
        }
        validateContent(content);
    }

    public void validateComment(String content) {
        validateContent(content);
    }

    private void validateContent(String content) {
        if (content == null || content.isBlank()) {
            throw badRequest("INVALID_CONTENT", "Content is required.");
        }
    }

    private ApiException badRequest(String code, String message) {
        return new ApiException(HttpStatus.BAD_REQUEST, code, message);
    }
}
