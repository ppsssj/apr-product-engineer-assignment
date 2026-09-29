package com.apr.community.post;

import java.util.List;

import com.apr.community.comment.CommentResponse;
import com.apr.community.common.error.ApiException;
import com.apr.community.common.pagination.CursorCodec;
import com.apr.community.common.pagination.CursorPosition;
import com.apr.community.common.pagination.PageRequest;
import com.apr.community.common.pagination.PageResponse;
import com.apr.community.persistence.CommentRepository;
import com.apr.community.persistence.PostRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class PostReadService {

    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final CursorCodec cursorCodec;

    public PostReadService(
            PostRepository postRepository,
            CommentRepository commentRepository,
            CursorCodec cursorCodec) {
        this.postRepository = postRepository;
        this.commentRepository = commentRepository;
        this.cursorCodec = cursorCodec;
    }

    public PageResponse<PostResponse> getPosts(String requester, PageRequest pageRequest) {
        var rows = postRepository.findReadablePage(
                requester,
                pageRequest.cursor(),
                pageRequest.limit() + 1);
        boolean hasNextPage = rows.size() > pageRequest.limit();
        var pageRows = rows.subList(0, Math.min(rows.size(), pageRequest.limit()));
        List<PostResponse> items = pageRows.stream()
                .map(this::toPostResponse)
                .toList();

        return new PageResponse<>(
                items,
                postRepository.countReadable(),
                nextCursor(pageRows, hasNextPage));
    }

    public PostResponse getPost(long postId, String requester) {
        return postRepository.findReadableById(postId, requester)
                .map(this::toPostResponse)
                .orElseThrow(() -> postNotFound(postId));
    }

    public PageResponse<CommentResponse> getComments(long postId, PageRequest pageRequest) {
        if (!postRepository.existsReadable(postId)) {
            throw postNotFound(postId);
        }

        var rows = commentRepository.findReadablePage(
                postId,
                pageRequest.cursor(),
                pageRequest.limit() + 1);
        boolean hasNextPage = rows.size() > pageRequest.limit();
        var pageRows = rows.subList(0, Math.min(rows.size(), pageRequest.limit()));
        List<CommentResponse> items = pageRows.stream()
                .map(this::toCommentResponse)
                .toList();

        return new PageResponse<>(
                items,
                commentRepository.countReadableForPost(postId),
                nextCommentCursor(pageRows, hasNextPage));
    }

    private String nextCursor(List<PostRepository.ReadablePost> rows, boolean hasNextPage) {
        if (!hasNextPage || rows.isEmpty()) {
            return null;
        }
        var last = rows.get(rows.size() - 1);
        return cursorCodec.encode(new CursorPosition(last.createdAt(), last.id()));
    }

    private String nextCommentCursor(
            List<CommentRepository.ReadableComment> rows,
            boolean hasNextPage) {
        if (!hasNextPage || rows.isEmpty()) {
            return null;
        }
        var last = rows.get(rows.size() - 1);
        return cursorCodec.encode(new CursorPosition(last.createdAt(), last.id()));
    }

    private PostResponse toPostResponse(PostRepository.ReadablePost post) {
        return new PostResponse(
                post.id(),
                post.title(),
                post.content(),
                post.likeCount(),
                post.commentCount(),
                post.isLiked(),
                post.author(),
                post.createdAt());
    }

    private CommentResponse toCommentResponse(CommentRepository.ReadableComment comment) {
        return new CommentResponse(
                comment.id(),
                comment.postId(),
                comment.content(),
                comment.author(),
                comment.createdAt());
    }

    private ApiException postNotFound(long postId) {
        return new ApiException(
                HttpStatus.NOT_FOUND,
                "POST_NOT_FOUND",
                "Post " + postId + " was not found.");
    }
}
