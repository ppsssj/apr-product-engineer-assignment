package com.apr.community;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;

import com.apr.community.comment.CommentCreateRequest;
import com.apr.community.comment.CommentMutationService;
import com.apr.community.common.error.ApiException;
import com.apr.community.common.pagination.PageRequestParser;
import com.apr.community.persistence.PostLikeRepository;
import com.apr.community.persistence.PostRepository;
import com.apr.community.post.PostMutationRequest;
import com.apr.community.post.PostMutationService;
import com.apr.community.post.PostReadService;
import com.apr.community.post.PostResponse;
import com.apr.community.support.PostgresIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;

class MutationPersistenceIntegrationTest extends PostgresIntegrationTest {

    private static final Instant T = Instant.parse("2099-01-01T00:00:00Z");
    private static final long MISSING_ID = Long.MAX_VALUE;

    @Autowired
    private PostMutationService postMutationService;

    @Autowired
    private CommentMutationService commentMutationService;

    @Autowired
    private PostReadService postReadService;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private PostLikeRepository postLikeRepository;

    @Autowired
    private PageRequestParser pageRequestParser;

    @Test
    void createdPostGetsAFreshIdentityAndIsReadableImmediately() {
        long maxIdBefore = maxId("posts");
        long readableBefore = postRepository.countReadable(null);

        PostResponse created = postMutationService.create(new PostMutationRequest("new title", "new body"), "alice");

        assertThat(created.id()).isGreaterThan(maxIdBefore);
        assertThat(created.author()).isEqualTo("alice");
        assertThat(created.likeCount()).isZero();
        assertThat(created.commentCount()).isZero();
        assertThat(postReadService.getPost(created.id(), "bob")).isEqualTo(created);
        assertThat(postRepository.countReadable(null)).isEqualTo(readableBefore + 1);
    }

    @Test
    void ownerUpdateChangesTextButKeepsIdentityAndOrderingKey() {
        long post = insertPost("before", "old body", "alice", 3, T, false);

        PostResponse updated = postMutationService.update(post, new PostMutationRequest("after", "new body"), "alice");

        assertThat(updated.id()).isEqualTo(post);
        assertThat(updated.title()).isEqualTo("after");
        assertThat(updated.content()).isEqualTo("new body");
        assertThat(updated.createdAt()).isEqualTo(T);
        assertThat(updated.likeCount()).isEqualTo(3);
        assertThat(postReadService.getPost(post, "alice")).isEqualTo(updated);
    }

    @Test
    void ownerDeleteKeepsThePhysicalRowButHidesThePost() {
        long post = insertPost("doomed", "x", "alice", 0, T, false);
        long readableBefore = postRepository.countReadable(null);

        postMutationService.delete(post, "alice");

        assertThat(isDeletedPost(post)).isTrue();
        assertThat(postRepository.existsReadable(post)).isFalse();
        assertThat(postRepository.countReadable(null)).isEqualTo(readableBefore - 1);
        assertStatus(() -> postReadService.getPost(post, "alice"), HttpStatus.NOT_FOUND);
    }

    @Test
    void commentCreateAndDeleteAdjustThePostAndCommentPage() {
        long post = insertPost("p", "x", "alice", 0, T, false);

        var comment = commentMutationService.create(post, new CommentCreateRequest("hello"), "bob");

        assertThat(comment.postId()).isEqualTo(post);
        assertThat(jdbcClient.sql("SELECT post_id FROM comments WHERE id = :id")
                .param("id", comment.id())
                .query(Long.class)
                .single()).isEqualTo(post);
        assertThat(postReadService.getPost(post, "alice").commentCount()).isEqualTo(1);
        var page = postReadService.getComments(post, pageRequestParser.parse(10, null));
        assertThat(page.items()).extracting(c -> c.id()).containsExactly(comment.id());
        assertThat(page.total()).isEqualTo(1);

        commentMutationService.delete(comment.id(), "bob");

        assertThat(postReadService.getPost(post, "alice").commentCount()).isZero();
        var afterDelete = postReadService.getComments(post, pageRequestParser.parse(10, null));
        assertThat(afterDelete.items()).isEmpty();
        assertThat(afterDelete.total()).isZero();
    }

    @Test
    void likeToggleCreatesAndRemovesTheRelationalRow() {
        long post = insertPost("p", "x", "alice", 4, T, false);

        var liked = postMutationService.toggleLike(post, "bob");

        assertThat(liked.isLiked()).isTrue();
        assertThat(liked.likeCount()).isEqualTo(5);
        assertThat(postLikeRepository.exists(post, "bob")).isTrue();
        var asCarol = postReadService.getPost(post, "carol");
        assertThat(asCarol.isLiked()).isFalse();
        assertThat(asCarol.likeCount()).isEqualTo(5);

        var unliked = postMutationService.toggleLike(post, "bob");

        assertThat(unliked.isLiked()).isFalse();
        assertThat(unliked.likeCount()).isEqualTo(4);
        assertThat(postLikeRepository.exists(post, "bob")).isFalse();
    }

    @Test
    void nonOwnerCannotUpdateOrDeleteAPost() {
        long post = insertPost("mine", "body", "alice", 0, T, false);

        assertStatus(() -> postMutationService.update(post, new PostMutationRequest("hijack", "x"), "mallory"),
                HttpStatus.FORBIDDEN);
        assertStatus(() -> postMutationService.delete(post, "mallory"), HttpStatus.FORBIDDEN);

        var unchanged = postReadService.getPost(post, "alice");
        assertThat(unchanged.title()).isEqualTo("mine");
        assertThat(unchanged.content()).isEqualTo("body");
        assertThat(isDeletedPost(post)).isFalse();
    }

    @Test
    void missingOrDeletedPostsAreNotFoundForEveryMutation() {
        long deleted = insertPost("gone", "x", "alice", 0, T, true);
        var request = new PostMutationRequest("t", "c");

        for (long post : new long[] {deleted, MISSING_ID}) {
            assertStatus(() -> postMutationService.update(post, request, "alice"), HttpStatus.NOT_FOUND);
            assertStatus(() -> postMutationService.delete(post, "alice"), HttpStatus.NOT_FOUND);
            assertStatus(() -> postMutationService.toggleLike(post, "alice"), HttpStatus.NOT_FOUND);
            assertStatus(() -> commentMutationService.create(post, new CommentCreateRequest("c"), "alice"),
                    HttpStatus.NOT_FOUND);
        }
        assertThat(postLikeRepository.exists(deleted, "alice")).isFalse();
    }

    @Test
    void commentDeleteRequiresOwnerAndAReadableCommentAndParent() {
        long post = insertPost("p", "x", "alice", 0, T, false);
        long comment = insertComment(post, "c", "bob", T, false);
        long deletedComment = insertComment(post, "c", "bob", T, true);
        long deletedParent = insertPost("gone", "x", "alice", 0, T, true);
        long orphaned = insertComment(deletedParent, "c", "bob", T, false);

        assertStatus(() -> commentMutationService.delete(comment, "mallory"), HttpStatus.FORBIDDEN);
        assertThat(postReadService.getPost(post, "alice").commentCount()).isEqualTo(1);

        assertStatus(() -> commentMutationService.delete(deletedComment, "bob"), HttpStatus.NOT_FOUND);
        assertStatus(() -> commentMutationService.delete(orphaned, "bob"), HttpStatus.NOT_FOUND);
        assertStatus(() -> commentMutationService.delete(MISSING_ID, "bob"), HttpStatus.NOT_FOUND);
    }

    private long maxId(String table) {
        return jdbcClient.sql("SELECT MAX(id) FROM " + table).query(Long.class).single();
    }

    private static void assertStatus(Runnable call, HttpStatus status) {
        assertThatThrownBy(call::run)
                .isInstanceOfSatisfying(ApiException.class,
                        exception -> assertThat(exception.status()).isEqualTo(status));
    }
}
