package com.apr.community.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;

import com.apr.community.support.PostgresIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;

// A constraint violation aborts the PostgreSQL transaction, so each test
// triggers exactly one, as its last statement.
class SchemaConstraintIntegrationTest extends PostgresIntegrationTest {

    private static final Instant T = Instant.parse("2099-01-01T00:00:00Z");

    @Autowired
    private PostLikeRepository postLikeRepository;

    @Autowired
    private CommentRepository commentRepository;

    @Test
    void sameUserCannotLikeThePostTwice() {
        long post = insertPost("p", "x", "u", 0, T, false);
        assertThat(postLikeRepository.insert(post, "alice")).isEqualTo(1);
        assertThat(postLikeRepository.insert(post, "bob")).isEqualTo(1);

        assertThatThrownBy(() -> postLikeRepository.insert(post, "alice"))
                .isInstanceOf(DuplicateKeyException.class);
    }

    @Test
    void commentRequiresAnExistingPost() {
        assertThatThrownBy(() -> commentRepository.insertComment(Long.MAX_VALUE, "c", "u", T))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("comments_post_id_fkey");
    }

    @Test
    void baseLikeCountCannotBeNegative() {
        assertThatThrownBy(() -> insertPost("p", "x", "u", -1, T, false))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("posts_base_like_count_check");
    }
}
