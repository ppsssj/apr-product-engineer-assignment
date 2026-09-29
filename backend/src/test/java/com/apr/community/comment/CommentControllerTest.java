package com.apr.community.comment;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.apr.community.common.error.ApiExceptionHandler;
import com.apr.community.common.requester.RequesterResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class CommentControllerTest {

    private CommentMutationService commentMutationService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        commentMutationService = mock(CommentMutationService.class);
        mockMvc = MockMvcBuilders
                .standaloneSetup(new CommentController(commentMutationService, new RequesterResolver()))
                .setControllerAdvice(new ApiExceptionHandler())
                .build();
    }

    @Test
    void deletesCommentAsRequesterAndReturnsNoContent() throws Exception {
        mockMvc.perform(delete("/api/comments/1").header("x-user-id", "owner"))
                .andExpect(status().isNoContent());

        verify(commentMutationService).delete(1L, "owner");
    }
}
