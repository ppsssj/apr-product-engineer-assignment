package com.apr.community.post;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;

import com.apr.community.comment.CommentResponse;
import com.apr.community.comment.CommentMutationService;
import com.apr.community.common.error.ApiException;
import com.apr.community.common.error.ApiExceptionHandler;
import com.apr.community.common.pagination.CursorCodec;
import com.apr.community.common.pagination.PageRequest;
import com.apr.community.common.pagination.PageRequestParser;
import com.apr.community.common.pagination.PageResponse;
import com.apr.community.common.requester.RequesterResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class PostControllerTest {

    private PostReadService postReadService;
    private PostMutationService postMutationService;
    private CommentMutationService commentMutationService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        postReadService = mock(PostReadService.class);
        postMutationService = mock(PostMutationService.class);
        commentMutationService = mock(CommentMutationService.class);
        var controller = new PostController(
                postReadService,
                postMutationService,
                commentMutationService,
                new RequesterResolver(),
                new PageRequestParser(new CursorCodec()));
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new ApiExceptionHandler())
                .build();
    }

    @Test
    void returnsPostListEnvelopeAndRequiredFields() throws Exception {
        var post = new PostResponse(
                42L,
                "title",
                "content",
                7L,
                3L,
                true,
                "requester",
                Instant.parse("2026-08-30T13:55:54.123456Z"));
        when(postReadService.getPosts(eq("requester"), any(PageRequest.class)))
                .thenReturn(new PageResponse<>(List.of(post), 39L, "next"));

        mockMvc.perform(get("/api/posts")
                        .header("x-user-id", "requester")
                        .param("limit", "2"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/json"))
                .andExpect(jsonPath("$.items[0].id").value(42))
                .andExpect(jsonPath("$.items[0].title").value("title"))
                .andExpect(jsonPath("$.items[0].content").value("content"))
                .andExpect(jsonPath("$.items[0].likeCount").value(7))
                .andExpect(jsonPath("$.items[0].commentCount").value(3))
                .andExpect(jsonPath("$.items[0].isLiked").value(true))
                .andExpect(jsonPath("$.items[0].author").value("requester"))
                .andExpect(jsonPath("$.items[0].createdAt").value("2026-08-30T13:55:54.123456Z"))
                .andExpect(jsonPath("$.total").value(39))
                .andExpect(jsonPath("$.nextCursor").value("next"));
    }

    @Test
    void returnsPostDetailAsSingleObject() throws Exception {
        var post = new PostResponse(
                1L,
                "title",
                "content",
                2L,
                3L,
                false,
                "author",
                Instant.parse("2026-08-30T13:55:54Z"));
        when(postReadService.getPost(1L, "apr_tester")).thenReturn(post);

        mockMvc.perform(get("/api/posts/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.isLiked").value(false))
                .andExpect(jsonPath("$.commentCount").value(3))
                .andExpect(jsonPath("$.items").doesNotExist());
    }

    @Test
    void returnsCommentEnvelopeAndRequiredFields() throws Exception {
        var comment = new CommentResponse(
                10L,
                1L,
                "comment",
                "author",
                Instant.parse("2026-08-30T14:02:33Z"));
        when(postReadService.getComments(eq(1L), any(PageRequest.class)))
                .thenReturn(new PageResponse<>(List.of(comment), 25L, null));

        mockMvc.perform(get("/api/posts/1/comments").param("limit", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].id").value(10))
                .andExpect(jsonPath("$.items[0].postId").value(1))
                .andExpect(jsonPath("$.items[0].content").value("comment"))
                .andExpect(jsonPath("$.items[0].author").value("author"))
                .andExpect(jsonPath("$.items[0].createdAt").value("2026-08-30T14:02:33Z"))
                .andExpect(jsonPath("$.total").value(25))
                .andExpect(jsonPath("$.nextCursor").value((Object) null));
    }

    @Test
    void rejectsLimitBelowRange() throws Exception {
        mockMvc.perform(get("/api/posts").param("limit", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_LIMIT"));

        verifyNoInteractions(postReadService);
    }

    @Test
    void rejectsLimitAboveRange() throws Exception {
        mockMvc.perform(get("/api/posts").param("limit", "51"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_LIMIT"));

        verifyNoInteractions(postReadService);
    }

    @Test
    void rejectsMalformedCursor() throws Exception {
        mockMvc.perform(get("/api/posts").param("cursor", "%%%"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_CURSOR"));

        verifyNoInteractions(postReadService);
    }

    @Test
    void returnsNotFoundForUnreadablePostDetail() throws Exception {
        when(postReadService.getPost(9L, "apr_tester"))
                .thenThrow(new ApiException(HttpStatus.NOT_FOUND, "POST_NOT_FOUND", "Post 9 was not found."));

        mockMvc.perform(get("/api/posts/9"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("POST_NOT_FOUND"));
    }

    @Test
    void returnsNotFoundForCommentsOfUnreadablePost() throws Exception {
        when(postReadService.getComments(eq(9L), any(PageRequest.class)))
                .thenThrow(new ApiException(HttpStatus.NOT_FOUND, "POST_NOT_FOUND", "Post 9 was not found."));

        mockMvc.perform(get("/api/posts/9/comments"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("POST_NOT_FOUND"));
    }

    @Test
    void returnsJsonErrorForMalformedPathId() throws Exception {
        mockMvc.perform(get("/api/posts/not-a-number"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_PARAMETER"));
    }

    @Test
    void createsPostWithRequesterAndCreatedStatus() throws Exception {
        var response = new PostResponse(43L, "title", "content", 0L, 0L, false, "author", Instant.EPOCH);
        when(postMutationService.create(any(PostMutationRequest.class), eq("author"))).thenReturn(response);

        mockMvc.perform(post("/api/posts")
                        .header("x-user-id", "author")
                        .contentType("application/json")
                        .content("{\"title\":\"title\",\"content\":\"content\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(43))
                .andExpect(jsonPath("$.author").value("author"))
                .andExpect(jsonPath("$.likeCount").value(0))
                .andExpect(jsonPath("$.commentCount").value(0))
                .andExpect(jsonPath("$.isLiked").value(false));
    }

    @Test
    void updatesAndDeletesPostWithDocumentedStatuses() throws Exception {
        var response = new PostResponse(1L, "updated", "content", 0L, 0L, false, "owner", Instant.EPOCH);
        when(postMutationService.update(eq(1L), any(PostMutationRequest.class), eq("owner")))
                .thenReturn(response);

        mockMvc.perform(patch("/api/posts/1")
                        .header("x-user-id", "owner")
                        .contentType("application/json")
                        .content("{\"title\":\"updated\",\"content\":\"content\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("updated"));

        mockMvc.perform(delete("/api/posts/1").header("x-user-id", "owner"))
                .andExpect(status().isNoContent());
        verify(postMutationService).delete(1L, "owner");
    }

    @Test
    void togglesLikeAndCreatesComment() throws Exception {
        when(postMutationService.toggleLike(1L, "apr_tester"))
                .thenReturn(new LikeToggleResponse(true, 8L));
        var comment = new CommentResponse(2077L, 1L, "comment", "apr_tester", Instant.EPOCH);
        when(commentMutationService.create(eq(1L), any(), eq("apr_tester"))).thenReturn(comment);

        mockMvc.perform(post("/api/posts/1/likes/toggle"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isLiked").value(true))
                .andExpect(jsonPath("$.likeCount").value(8));

        mockMvc.perform(post("/api/posts/1/comments")
                        .contentType("application/json")
                        .content("{\"content\":\"comment\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(2077));
    }

    @Test
    void rejectsMalformedJsonWithJsonError() throws Exception {
        mockMvc.perform(post("/api/posts")
                        .contentType("application/json")
                        .content("{not-json"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith("application/json"))
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
    }

    @Test
    void mapsValidationOwnershipAndMissingErrorsToJsonContract() throws Exception {
        when(postMutationService.create(any(PostMutationRequest.class), eq("apr_tester")))
                .thenThrow(new ApiException(HttpStatus.BAD_REQUEST, "INVALID_TITLE", "Title is required."));
        when(postMutationService.update(eq(1L), any(PostMutationRequest.class), eq("other")))
                .thenThrow(new ApiException(HttpStatus.FORBIDDEN, "FORBIDDEN", "Only the author may modify this post."));
        when(postMutationService.toggleLike(9L, "apr_tester"))
                .thenThrow(new ApiException(HttpStatus.NOT_FOUND, "POST_NOT_FOUND", "Post 9 was not found."));

        mockMvc.perform(post("/api/posts")
                        .contentType("application/json")
                        .content("{\"title\":\"\",\"content\":\"content\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_TITLE"));

        mockMvc.perform(patch("/api/posts/1")
                        .header("x-user-id", "other")
                        .contentType("application/json")
                        .content("{\"title\":\"title\",\"content\":\"content\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));

        mockMvc.perform(post("/api/posts/9/likes/toggle"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("POST_NOT_FOUND"));
    }
}
