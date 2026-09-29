package com.apr.community.comment;

import java.time.Instant;

import com.apr.community.common.error.ApiException;
import com.apr.community.common.validation.MutationValidator;
import com.apr.community.persistence.CommentRepository;
import com.apr.community.persistence.PostRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CommentMutationService {

    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final MutationValidator validator;

    public CommentMutationService(
            PostRepository postRepository,
            CommentRepository commentRepository,
            MutationValidator validator) {
        this.postRepository = postRepository;
        this.commentRepository = commentRepository;
        this.validator = validator;
    }

    @Transactional
    public CommentResponse create(long postId, CommentCreateRequest request, String requester) {
        if (request == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", "Request body is invalid.");
        }
        validator.validateComment(request.content());
        lockReadablePost(postId);
        Instant createdAt = Instant.now();
        long commentId = commentRepository.insertComment(postId, request.content(), requester, createdAt);
        return new CommentResponse(commentId, postId, request.content(), requester, createdAt);
    }

    @Transactional
    public void delete(long commentId, String requester) {
        var target = commentRepository.findReadableMutationTargetForUpdate(commentId)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        "COMMENT_NOT_FOUND",
                        "Comment " + commentId + " was not found."));
        if (!target.author().equals(requester)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "FORBIDDEN", "Only the author may delete this comment.");
        }
        commentRepository.softDelete(commentId);
    }

    private void lockReadablePost(long postId) {
        postRepository.findReadableMutationTargetForUpdate(postId)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        "POST_NOT_FOUND",
                        "Post " + postId + " was not found."));
    }
}
