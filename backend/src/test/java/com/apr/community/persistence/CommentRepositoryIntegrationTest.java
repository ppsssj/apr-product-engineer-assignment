package com.apr.community.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import com.apr.community.comment.CommentResponse;
import com.apr.community.common.pagination.PageRequestParser;
import com.apr.community.common.pagination.PageResponse;
import com.apr.community.post.PostReadService;
import com.apr.community.support.PostgresIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class CommentRepositoryIntegrationTest extends PostgresIntegrationTest {

    private static final Instant T = Instant.parse("2099-01-01T00:00:00Z");

    @Autowired
    private PostReadService postReadService;

    @Autowired
    private PageRequestParser pageRequestParser;

    @Autowired
    private CommentRepository commentRepository;

    @Test
    void commentPagesFollowCreatedAtThenIdAndSkipDeletedComments() {
        long post = insertPost("p", "x", "u", 0, T, false);
        long otherPost = insertPost("other", "x", "u", 0, T, false);
        long older = insertComment(post, "older", "u", T.minusSeconds(1), false);
        long tie1 = insertComment(post, "tie1", "u", T, false);
        long tie2 = insertComment(post, "tie2", "u", T, false);
        long deletedTie = insertComment(post, "deleted", "u", T, true);
        long tie3 = insertComment(post, "tie3", "u", T, false);
        long tie4 = insertComment(post, "tie4", "u", T, false);
        long newer = insertComment(post, "newer", "u", T.plusSeconds(1), false);
        long foreign = insertComment(otherPost, "foreign", "u", T, false);

        List<CommentResponse> traversed = traverse(post, 2, 3, 1);

        assertThat(traversed).extracting(CommentResponse::id)
                .doesNotHaveDuplicates()
                .doesNotContain(deletedTie, foreign)
                .containsExactly(newer, tie4, tie3, tie2, tie1, older);
        assertThat(traversed).extracting(CommentResponse::postId).containsOnly(post);
        assertThat(postReadService.getPost(post, "u").commentCount()).isEqualTo(6);
    }

    @Test
    void deletingACommentRemovesItFromPagesTotalAndPostCount() {
        long post = insertPost("p", "x", "u", 0, T, false);
        long kept = insertComment(post, "kept", "u", T, false);
        long removed = insertComment(post, "removed", "u", T.plusSeconds(1), false);

        commentRepository.softDelete(removed);

        PageResponse<CommentResponse> page = postReadService.getComments(post, pageRequestParser.parse(10, null));
        assertThat(page.items()).extracting(CommentResponse::id).containsExactly(kept);
        assertThat(page.total()).isEqualTo(1);
        assertThat(page.nextCursor()).isNull();
        assertThat(commentRepository.countReadableForPost(post)).isEqualTo(1);
        assertThat(postReadService.getPost(post, "u").commentCount()).isEqualTo(1);
    }

    @Test
    void seededCommentPagesCoverEveryReadableCommentOnce() {
        long post = jdbcClient.sql("""
                        SELECT c.post_id FROM comments c
                        JOIN posts p ON p.id = c.post_id AND p.is_deleted = FALSE
                        GROUP BY c.post_id
                        HAVING bool_or(c.is_deleted)
                        ORDER BY COUNT(*) DESC
                        LIMIT 1
                        """)
                .query(Long.class)
                .single();
        List<Long> readableIds = jdbcClient.sql("""
                        SELECT id FROM comments WHERE post_id = :postId AND is_deleted = FALSE
                        """)
                .param("postId", post)
                .query(Long.class)
                .list();

        List<CommentResponse> traversed = traverse(post, 7, 13);

        assertThat(traversed).extracting(CommentResponse::id)
                .doesNotHaveDuplicates()
                .containsExactlyInAnyOrderElementsOf(readableIds);
        for (int i = 1; i < traversed.size(); i++) {
            var previous = traversed.get(i - 1);
            var current = traversed.get(i);
            assertThat(previous.createdAt().isAfter(current.createdAt())
                    || previous.createdAt().equals(current.createdAt()) && previous.id() > current.id())
                    .as("order at index %d", i)
                    .isTrue();
        }
    }

    // Follows nextCursor to the end, cycling through the given page sizes.
    private List<CommentResponse> traverse(long postId, int... limits) {
        List<CommentResponse> items = new ArrayList<>();
        String cursor = null;
        Long total = null;
        for (int page = 0; ; page++) {
            assertThat(page).as("page count").isLessThan(500);
            int limit = limits[page % limits.length];
            PageResponse<CommentResponse> response =
                    postReadService.getComments(postId, pageRequestParser.parse(limit, cursor));

            if (total == null) {
                total = response.total();
            }
            assertThat(response.total()).isEqualTo(total);
            assertThat(response.items()).hasSizeLessThanOrEqualTo(limit);
            items.addAll(response.items());
            if (response.nextCursor() == null) {
                break;
            }
            assertThat(response.items()).hasSize(limit);
            cursor = response.nextCursor();
        }
        assertThat(items).hasSize(total.intValue());
        return items;
    }
}
