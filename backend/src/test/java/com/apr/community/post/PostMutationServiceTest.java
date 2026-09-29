package com.apr.community.post;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;

import com.apr.community.common.error.ApiException;
import com.apr.community.common.validation.MutationValidator;
import com.apr.community.persistence.PostLikeRepository;
import com.apr.community.persistence.PostRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PostMutationServiceTest {

    private PostRepository postRepository;
    private PostLikeRepository postLikeRepository;
    private PostReadService postReadService;
    private PostMutationService service;

    @BeforeEach
    void setUp() {
        postRepository = mock(PostRepository.class);
        postLikeRepository = mock(PostLikeRepository.class);
        postReadService = mock(PostReadService.class);
        service = new PostMutationService(
                postRepository,
                postLikeRepository,
                postReadService,
                new MutationValidator());
    }

    @Test
    void createsPostWithoutTrimmingStoredValues() {
        var expected = post(43L, false, 0L);
        when(postRepository.insertPost(org.mockito.ArgumentMatchers.eq(" title "),
                org.mockito.ArgumentMatchers.eq(" content "),
                org.mockito.ArgumentMatchers.eq("author"), any(Instant.class))).thenReturn(43L);
        when(postReadService.getPost(43L, "author")).thenReturn(expected);

        assertEquals(expected, service.create(new PostMutationRequest(" title ", " content "), "author"));
        verify(postRepository).insertPost(org.mockito.ArgumentMatchers.eq(" title "),
                org.mockito.ArgumentMatchers.eq(" content "),
                org.mockito.ArgumentMatchers.eq("author"), any(Instant.class));
    }

    @Test
    void rejectsUpdateByNonOwner() {
        when(postRepository.findReadableMutationTargetForUpdate(1L))
                .thenReturn(Optional.of(new PostRepository.PostMutationTarget(1L, "owner")));

        ApiException exception = assertThrows(ApiException.class,
                () -> service.update(1L, new PostMutationRequest("title", "content"), "other"));

        assertEquals(403, exception.status().value());
        verify(postRepository, never()).updateReadablePost(anyLong(), any(), any());
    }

    @Test
    void ownerUpdatesPostAndReceivesStandardReadResponse() {
        var expected = post(1L, false, 7L);
        when(postRepository.findReadableMutationTargetForUpdate(1L))
                .thenReturn(Optional.of(new PostRepository.PostMutationTarget(1L, "owner")));
        when(postReadService.getPost(1L, "owner")).thenReturn(expected);

        assertEquals(expected, service.update(1L, new PostMutationRequest("new", "body"), "owner"));

        verify(postRepository).updateReadablePost(1L, "new", "body");
    }

    @Test
    void ownerSoftDeletesPost() {
        when(postRepository.findReadableMutationTargetForUpdate(1L))
                .thenReturn(Optional.of(new PostRepository.PostMutationTarget(1L, "owner")));

        service.delete(1L, "owner");

        verify(postRepository).softDelete(1L);
    }

    @Test
    void treatsDeletedOrMissingPostAsNotFound() {
        when(postRepository.findReadableMutationTargetForUpdate(9L)).thenReturn(Optional.empty());

        ApiException exception = assertThrows(ApiException.class, () -> service.delete(9L, "owner"));

        assertEquals("POST_NOT_FOUND", exception.code());
        assertEquals(404, exception.status().value());
    }

    @Test
    void togglesLikeOnAndReturnsDerivedState() {
        when(postRepository.findReadableMutationTargetForUpdate(1L))
                .thenReturn(Optional.of(new PostRepository.PostMutationTarget(1L, "owner")));
        when(postLikeRepository.exists(1L, "requester")).thenReturn(false);
        when(postReadService.getPost(1L, "requester")).thenReturn(post(1L, true, 8L));

        LikeToggleResponse response = service.toggleLike(1L, "requester");

        verify(postLikeRepository).insert(1L, "requester");
        assertTrue(response.isLiked());
        assertEquals(8L, response.likeCount());
    }

    @Test
    void togglesLikeOffAndReturnsDerivedState() {
        when(postRepository.findReadableMutationTargetForUpdate(1L))
                .thenReturn(Optional.of(new PostRepository.PostMutationTarget(1L, "owner")));
        when(postLikeRepository.exists(1L, "requester")).thenReturn(true);
        when(postReadService.getPost(1L, "requester")).thenReturn(post(1L, false, 7L));

        LikeToggleResponse response = service.toggleLike(1L, "requester");

        verify(postLikeRepository).delete(1L, "requester");
        assertFalse(response.isLiked());
        assertEquals(7L, response.likeCount());
    }

    @Test
    void rejectsLikeToggleForDeletedOrMissingPost() {
        when(postRepository.findReadableMutationTargetForUpdate(9L)).thenReturn(Optional.empty());

        ApiException exception = assertThrows(ApiException.class, () -> service.toggleLike(9L, "requester"));

        assertEquals(404, exception.status().value());
        verify(postLikeRepository, never()).insert(anyLong(), any());
    }

    private PostResponse post(long id, boolean isLiked, long likeCount) {
        return new PostResponse(id, "title", "content", likeCount, 0L, isLiked, "owner", Instant.EPOCH);
    }
}
