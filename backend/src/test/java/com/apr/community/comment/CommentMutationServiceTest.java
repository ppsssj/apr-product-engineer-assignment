package com.apr.community.comment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;

import com.apr.community.common.error.ApiException;
import com.apr.community.common.validation.MutationValidator;
import com.apr.community.persistence.CommentRepository;
import com.apr.community.persistence.PostRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CommentMutationServiceTest {

    private PostRepository postRepository;
    private CommentRepository commentRepository;
    private CommentMutationService service;

    @BeforeEach
    void setUp() {
        postRepository = mock(PostRepository.class);
        commentRepository = mock(CommentRepository.class);
        service = new CommentMutationService(postRepository, commentRepository, new MutationValidator());
    }

    @Test
    void createsCommentForReadablePost() {
        when(postRepository.findReadableMutationTargetForUpdate(1L))
                .thenReturn(Optional.of(new PostRepository.PostMutationTarget(1L, "post-owner")));
        when(commentRepository.insertComment(eq(1L), eq(" comment "), eq("author"), any(Instant.class)))
                .thenReturn(2077L);

        CommentResponse response = service.create(1L, new CommentCreateRequest(" comment "), "author");

        assertEquals(2077L, response.id());
        assertEquals(" comment ", response.content());
        assertEquals("author", response.author());
    }

    @Test
    void rejectsDeleteByNonOwner() {
        when(commentRepository.findReadableMutationTargetForUpdate(1L))
                .thenReturn(Optional.of(new CommentRepository.CommentMutationTarget(1L, 1L, "owner")));

        ApiException exception = assertThrows(ApiException.class, () -> service.delete(1L, "other"));

        assertEquals(403, exception.status().value());
        verify(commentRepository, never()).softDelete(1L);
    }

    @Test
    void commentOwnerSoftDeletesEvenWhenTheyDoNotOwnThePost() {
        when(commentRepository.findReadableMutationTargetForUpdate(1L))
                .thenReturn(Optional.of(new CommentRepository.CommentMutationTarget(1L, 10L, "comment-owner")));

        service.delete(1L, "comment-owner");

        verify(commentRepository).softDelete(1L);
    }

    @Test
    void rejectsCreationForDeletedOrMissingParent() {
        when(postRepository.findReadableMutationTargetForUpdate(9L)).thenReturn(Optional.empty());

        ApiException exception = assertThrows(ApiException.class,
                () -> service.create(9L, new CommentCreateRequest("comment"), "author"));

        assertEquals("POST_NOT_FOUND", exception.code());
        assertEquals(404, exception.status().value());
    }

    @Test
    void treatsDeletedOrMissingCommentAsNotFound() {
        when(commentRepository.findReadableMutationTargetForUpdate(9L)).thenReturn(Optional.empty());

        ApiException exception = assertThrows(ApiException.class, () -> service.delete(9L, "owner"));

        assertEquals("COMMENT_NOT_FOUND", exception.code());
        assertEquals(404, exception.status().value());
    }
}
