package com.apr.community.post;

import com.apr.community.comment.CommentResponse;
import com.apr.community.common.pagination.PageRequestParser;
import com.apr.community.common.pagination.PageResponse;
import com.apr.community.common.requester.RequesterResolver;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/posts")
public class PostController {

    private final PostReadService postReadService;
    private final RequesterResolver requesterResolver;
    private final PageRequestParser pageRequestParser;

    public PostController(
            PostReadService postReadService,
            RequesterResolver requesterResolver,
            PageRequestParser pageRequestParser) {
        this.postReadService = postReadService;
        this.requesterResolver = requesterResolver;
        this.pageRequestParser = pageRequestParser;
    }

    @GetMapping
    public PageResponse<PostResponse> getPosts(
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) String cursor,
            HttpServletRequest request) {
        return postReadService.getPosts(
                requesterResolver.resolve(request),
                pageRequestParser.parse(limit, cursor));
    }

    @GetMapping("/{postId}")
    public PostResponse getPost(
            @PathVariable long postId,
            HttpServletRequest request) {
        return postReadService.getPost(postId, requesterResolver.resolve(request));
    }

    @GetMapping("/{postId}/comments")
    public PageResponse<CommentResponse> getComments(
            @PathVariable long postId,
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) String cursor) {
        return postReadService.getComments(postId, pageRequestParser.parse(limit, cursor));
    }
}
