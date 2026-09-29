package com.apr.community.post;

import java.time.Instant;

import com.apr.community.common.error.ApiException;
import com.apr.community.common.validation.MutationValidator;
import com.apr.community.persistence.PostLikeRepository;
import com.apr.community.persistence.PostRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PostMutationService {

    private final PostRepository postRepository;
    private final PostLikeRepository postLikeRepository;
    private final PostReadService postReadService;
    private final MutationValidator validator;

    public PostMutationService(
            PostRepository postRepository,
            PostLikeRepository postLikeRepository,
            PostReadService postReadService,
            MutationValidator validator) {
        this.postRepository = postRepository;
        this.postLikeRepository = postLikeRepository;
        this.postReadService = postReadService;
        this.validator = validator;
    }

    @Transactional
    public PostResponse create(PostMutationRequest request, String requester) {
        requireRequest(request);
        validator.validatePost(request.title(), request.content());
        long postId = postRepository.insertPost(request.title(), request.content(), requester, Instant.now());
        return postReadService.getPost(postId, requester);
    }

    @Transactional
    public PostResponse update(long postId, PostMutationRequest request, String requester) {
        requireRequest(request);
        validator.validatePost(request.title(), request.content());
        var target = lockReadablePost(postId);
        requireOwner(target.author(), requester);
        postRepository.updateReadablePost(postId, request.title(), request.content());
        return postReadService.getPost(postId, requester);
    }

    @Transactional
    public void delete(long postId, String requester) {
        var target = lockReadablePost(postId);
        requireOwner(target.author(), requester);
        postRepository.softDelete(postId);
    }

    @Transactional
    public LikeToggleResponse toggleLike(long postId, String requester) {
        lockReadablePost(postId);
        if (postLikeRepository.exists(postId, requester)) {
            postLikeRepository.delete(postId, requester);
        } else {
            postLikeRepository.insert(postId, requester);
        }
        PostResponse post = postReadService.getPost(postId, requester);
        return new LikeToggleResponse(post.isLiked(), post.likeCount());
    }

    private PostRepository.PostMutationTarget lockReadablePost(long postId) {
        return postRepository.findReadableMutationTargetForUpdate(postId)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        "POST_NOT_FOUND",
                        "Post " + postId + " was not found."));
    }

    private void requireOwner(String author, String requester) {
        if (!author.equals(requester)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "FORBIDDEN", "Only the author may modify this post.");
        }
    }

    private void requireRequest(PostMutationRequest request) {
        if (request == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", "Request body is invalid.");
        }
    }
}
