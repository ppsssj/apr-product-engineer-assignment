package com.apr.community.comment;

import com.apr.community.common.requester.RequesterResolver;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/comments")
public class CommentController {

    private final CommentMutationService commentMutationService;
    private final RequesterResolver requesterResolver;

    public CommentController(
            CommentMutationService commentMutationService,
            RequesterResolver requesterResolver) {
        this.commentMutationService = commentMutationService;
        this.requesterResolver = requesterResolver;
    }

    @DeleteMapping("/{commentId}")
    public ResponseEntity<Void> deleteComment(
            @PathVariable long commentId,
            HttpServletRequest request) {
        commentMutationService.delete(commentId, requesterResolver.resolve(request));
        return ResponseEntity.noContent().build();
    }
}
