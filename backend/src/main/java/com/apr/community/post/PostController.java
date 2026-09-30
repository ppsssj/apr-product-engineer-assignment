package com.apr.community.post;

import com.apr.community.comment.CommentCreateRequest;
import com.apr.community.comment.CommentMutationService;
import com.apr.community.comment.CommentResponse;
import com.apr.community.common.pagination.PageRequestParser;
import com.apr.community.common.pagination.PageResponse;
import com.apr.community.common.requester.RequesterResolver;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/posts")
public class PostController {

    private final PostReadService postReadService;
    private final PostMutationService postMutationService;
    private final CommentMutationService commentMutationService;
    private final RequesterResolver requesterResolver;
    private final PageRequestParser pageRequestParser;
    private final PostListRequestParser postListRequestParser;

    public PostController(
            PostReadService postReadService,
            PostMutationService postMutationService,
            CommentMutationService commentMutationService,
            RequesterResolver requesterResolver,
            PageRequestParser pageRequestParser,
            PostListRequestParser postListRequestParser) {
        this.postReadService = postReadService;
        this.postMutationService = postMutationService;
        this.commentMutationService = commentMutationService;
        this.requesterResolver = requesterResolver;
        this.pageRequestParser = pageRequestParser;
        this.postListRequestParser = postListRequestParser;
    }

    // `sort` (latest | likes, default latest) and `q` (title/content search)
    // extend the original limit/cursor contract without changing its default.
    @GetMapping
    public PageResponse<PostResponse> getPosts(
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) String cursor,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) String q,
            HttpServletRequest request) {
        return postReadService.getPosts(
                requesterResolver.resolve(request),
                postListRequestParser.parse(limit, cursor, sort, q));
    }

    @PostMapping
    public ResponseEntity<PostResponse> createPost(
            @RequestBody PostMutationRequest body,
            HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(postMutationService.create(body, requesterResolver.resolve(request)));
    }

    @GetMapping("/{postId}")
    public PostResponse getPost(
            @PathVariable long postId,
            HttpServletRequest request) {
        return postReadService.getPost(postId, requesterResolver.resolve(request));
    }

    @PatchMapping("/{postId}")
    public PostResponse updatePost(
            @PathVariable long postId,
            @RequestBody PostMutationRequest body,
            HttpServletRequest request) {
        return postMutationService.update(postId, body, requesterResolver.resolve(request));
    }

    @DeleteMapping("/{postId}")
    public ResponseEntity<Void> deletePost(
            @PathVariable long postId,
            HttpServletRequest request) {
        postMutationService.delete(postId, requesterResolver.resolve(request));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{postId}/likes/toggle")
    public LikeToggleResponse toggleLike(
            @PathVariable long postId,
            HttpServletRequest request) {
        return postMutationService.toggleLike(postId, requesterResolver.resolve(request));
    }

    @GetMapping("/{postId}/comments")
    public PageResponse<CommentResponse> getComments(
            @PathVariable long postId,
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) String cursor) {
        return postReadService.getComments(postId, pageRequestParser.parse(limit, cursor));
    }

    @PostMapping("/{postId}/comments")
    public ResponseEntity<CommentResponse> createComment(
            @PathVariable long postId,
            @RequestBody CommentCreateRequest body,
            HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(commentMutationService.create(postId, body, requesterResolver.resolve(request)));
    }
}
