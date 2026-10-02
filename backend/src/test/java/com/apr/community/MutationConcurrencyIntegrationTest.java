package com.apr.community;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.sql.DataSource;

import com.apr.community.comment.CommentCreateRequest;
import com.apr.community.comment.CommentMutationService;
import com.apr.community.common.error.ApiException;
import com.apr.community.post.LikeToggleResponse;
import com.apr.community.post.PostMutationRequest;
import com.apr.community.post.PostMutationService;
import com.apr.community.post.PostReadService;
import com.apr.community.support.PostgresIntegrationTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

// Mutations racing on one post, each in its own real transaction (no outer
// test transaction, so fixtures are committed and cleaned up explicitly).
// Races are made deterministic by a separate JDBC transaction that holds the
// post row lock: the workers are released only after PostgreSQL reports them
// waiting on a lock, so they provably overlap on the FOR UPDATE in the service.
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class MutationConcurrencyIntegrationTest extends PostgresIntegrationTest {

    private static final Instant T = Instant.parse("2099-01-01T00:00:00Z");
    private static final long TIMEOUT_SECONDS = 10;

    @Autowired
    private PostMutationService postMutationService;

    @Autowired
    private CommentMutationService commentMutationService;

    @Autowired
    private PostReadService postReadService;

    @Autowired
    private DataSource dataSource;

    private final List<Long> fixturePosts = new ArrayList<>();
    private ExecutorService executor;

    @BeforeEach
    void startExecutor() {
        executor = Executors.newFixedThreadPool(4);
    }

    @AfterEach
    void cleanUp() throws InterruptedException {
        executor.shutdownNow();
        assertThat(executor.awaitTermination(TIMEOUT_SECONDS, TimeUnit.SECONDS)).as("executor terminated").isTrue();
        for (long postId : fixturePosts) {
            jdbcClient.sql("DELETE FROM post_likes WHERE post_id = :id").param("id", postId).update();
            jdbcClient.sql("DELETE FROM comments WHERE post_id = :id").param("id", postId).update();
            jdbcClient.sql("DELETE FROM posts WHERE id = :id").param("id", postId).update();
        }
    }

    @Test
    void sameUserTogglesSerializeIntoLikeThenUnlike() throws Exception {
        long post = fixturePost("alice", 5);

        List<Outcome<LikeToggleResponse>> outcomes = whileRowLocked(post, null,
                () -> postMutationService.toggleLike(post, "bob"),
                () -> postMutationService.toggleLike(post, "bob"));

        assertThat(outcomes).allSatisfy(Outcome::assertSucceeded);
        assertThat(outcomes).extracting(outcome -> outcome.value().isLiked())
                .containsExactlyInAnyOrder(true, false);
        assertThat(outcomes).extracting(outcome -> outcome.value().likeCount())
                .containsExactlyInAnyOrder(6L, 5L);
        assertThat(likeRows(post, "bob")).isZero();
        var finalState = postReadService.getPost(post, "bob");
        assertThat(finalState.isLiked()).isFalse();
        assertThat(finalState.likeCount()).isEqualTo(5);
    }

    @Test
    void differentUsersLikesAreBothApplied() throws Exception {
        long post = fixturePost("alice", 5);

        List<Outcome<LikeToggleResponse>> outcomes = whileRowLocked(post, null,
                () -> postMutationService.toggleLike(post, "bob"),
                () -> postMutationService.toggleLike(post, "carol"));

        assertThat(outcomes).allSatisfy(Outcome::assertSucceeded);
        assertThat(outcomes).extracting(outcome -> outcome.value().isLiked()).containsOnly(true);
        // Serialized on the post row: the second toggle observes the first like.
        assertThat(outcomes).extracting(outcome -> outcome.value().likeCount())
                .containsExactlyInAnyOrder(6L, 7L);
        assertThat(likeRows(post, "bob")).isOne();
        assertThat(likeRows(post, "carol")).isOne();
        assertThat(postReadService.getPost(post, "bob").isLiked()).isTrue();
        assertThat(postReadService.getPost(post, "carol").isLiked()).isTrue();
        var asDave = postReadService.getPost(post, "dave");
        assertThat(asDave.isLiked()).isFalse();
        assertThat(asDave.likeCount()).isEqualTo(7);
    }

    @Test
    void concurrentCommentCreatesAreBothStored() throws Exception {
        long post = fixturePost("alice", 0);

        var outcomes = whileRowLocked(post, null,
                () -> commentMutationService.create(post, new CommentCreateRequest("from bob"), "bob"),
                () -> commentMutationService.create(post, new CommentCreateRequest("from carol"), "carol"));

        assertThat(outcomes).allSatisfy(Outcome::assertSucceeded);
        assertThat(outcomes).extracting(outcome -> outcome.value().id()).doesNotHaveDuplicates();
        assertThat(readableComments(post)).isEqualTo(2);
        assertThat(postReadService.getPost(post, "alice").commentCount()).isEqualTo(2);
    }

    @Test
    void deleteCommittedWhileCommentCreateWaitsMakesItNotFound() throws Exception {
        long post = fixturePost("alice", 0);

        var outcomes = whileRowLocked(post, "UPDATE posts SET is_deleted = TRUE WHERE id = ?",
                () -> commentMutationService.create(post, new CommentCreateRequest("late"), "bob"));

        outcomes.get(0).assertFailedWith(HttpStatus.NOT_FOUND, "POST_NOT_FOUND");
        assertThat(jdbcClient.sql("SELECT COUNT(*) FROM comments WHERE post_id = :id")
                .param("id", post).query(Integer.class).single()).isZero();
        assertThat(isDeletedPost(post)).isTrue();
    }

    @Test
    void deleteCommittedWhileLikeWaitsMakesItNotFound() throws Exception {
        long post = fixturePost("alice", 5);

        var outcomes = whileRowLocked(post, "UPDATE posts SET is_deleted = TRUE WHERE id = ?",
                () -> postMutationService.toggleLike(post, "bob"));

        outcomes.get(0).assertFailedWith(HttpStatus.NOT_FOUND, "POST_NOT_FOUND");
        assertThat(likeRows(post, "bob")).isZero();
        assertThat(isDeletedPost(post)).isTrue();
    }

    @Test
    void deleteCommittedWhileUpdateWaitsMakesItNotFound() throws Exception {
        long post = fixturePost("alice", 0);

        var outcomes = whileRowLocked(post, "UPDATE posts SET is_deleted = TRUE WHERE id = ?",
                () -> postMutationService.update(post, new PostMutationRequest("late title", "late body"), "alice"));

        outcomes.get(0).assertFailedWith(HttpStatus.NOT_FOUND, "POST_NOT_FOUND");
        assertThat(isDeletedPost(post)).isTrue();
        assertThat(jdbcClient.sql("SELECT title FROM posts WHERE id = :id")
                .param("id", post).query(String.class).single()).isEqualTo("fixture");
    }

    @Test
    void concurrentDeletesByOwnerSucceedExactlyOnce() throws Exception {
        long post = fixturePost("alice", 0);

        List<Outcome<Boolean>> outcomes = whileRowLocked(post, null,
                () -> {
                    postMutationService.delete(post, "alice");
                    return true;
                },
                () -> {
                    postMutationService.delete(post, "alice");
                    return true;
                });

        assertThat(outcomes.stream().filter(Outcome::succeeded)).hasSize(1);
        outcomes.stream().filter(outcome -> !outcome.succeeded()).findFirst().orElseThrow()
                .assertFailedWith(HttpStatus.NOT_FOUND, "POST_NOT_FOUND");
        assertThat(isDeletedPost(post)).isTrue();
    }

    // Unforced race, repeated: post delete takes only the post row lock and
    // comment delete takes post + comment, so no lock cycle exists. Either
    // order is valid; each run must end in one of the two linearizable states.
    @Test
    void postDeleteRacingCommentDeleteEndsInALinearizableState() throws Exception {
        int commentFirst = 0;
        int postFirst = 0;
        for (int run = 0; run < 20; run++) {
            long post = fixturePost("alice", 0);
            long comment = insertComment(post, "mine", "alice", T, false);
            var barrier = new CyclicBarrier(2);

            Future<Outcome<Boolean>> deletePost = executor.submit(() -> {
                barrier.await(TIMEOUT_SECONDS, TimeUnit.SECONDS);
                return Outcome.of(() -> {
                    postMutationService.delete(post, "alice");
                    return true;
                });
            });
            Future<Outcome<Boolean>> deleteComment = executor.submit(() -> {
                barrier.await(TIMEOUT_SECONDS, TimeUnit.SECONDS);
                return Outcome.of(() -> {
                    commentMutationService.delete(comment, "alice");
                    return true;
                });
            });
            var postOutcome = await(deletePost, "post delete");
            var commentOutcome = await(deleteComment, "comment delete");

            postOutcome.assertSucceeded();
            assertThat(isDeletedPost(post)).isTrue();
            boolean commentDeleted = jdbcClient.sql("SELECT is_deleted FROM comments WHERE id = :id")
                    .param("id", comment).query(Boolean.class).single();
            if (commentOutcome.succeeded()) {
                assertThat(commentDeleted).as("comment delete reported success").isTrue();
                commentFirst++;
            } else {
                commentOutcome.assertFailedWith(HttpStatus.NOT_FOUND, "COMMENT_NOT_FOUND");
                assertThat(commentDeleted).as("comment delete lost to post delete").isFalse();
                postFirst++;
            }
        }
        System.out.printf("post delete vs comment delete: comment-first=%d post-first=%d%n", commentFirst, postFirst);
        assertThat(commentFirst + postFirst).isEqualTo(20);
    }

    // Runs the workers while a separate transaction holds the post row lock.
    // They are released only once all of them are blocked on a lock; the holder
    // then runs `holderSql` (if any) and commits, so its change is linearized
    // before every worker.
    @SafeVarargs
    private <T> List<Outcome<T>> whileRowLocked(long postId, String holderSql, Callable<T>... workers)
            throws Exception {
        List<Future<Outcome<T>>> futures = new ArrayList<>();
        try (Connection holder = dataSource.getConnection()) {
            holder.setAutoCommit(false);
            try {
                execute(holder, "SELECT id FROM posts WHERE id = ? FOR UPDATE", postId);
                for (Callable<T> worker : workers) {
                    futures.add(executor.submit(() -> Outcome.of(worker)));
                }
                awaitLockWaiters(workers.length);
                if (holderSql != null) {
                    execute(holder, holderSql, postId);
                }
                holder.commit();
            } catch (Exception exception) {
                holder.rollback();
                throw exception;
            }
        }
        List<Outcome<T>> outcomes = new ArrayList<>();
        for (int i = 0; i < futures.size(); i++) {
            outcomes.add(await(futures.get(i), "worker " + i));
        }
        return outcomes;
    }

    private void awaitLockWaiters(int expected) throws InterruptedException {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(TIMEOUT_SECONDS);
        int waiting = 0;
        while (System.nanoTime() < deadline) {
            waiting = jdbcClient.sql("""
                            SELECT COUNT(*) FROM pg_stat_activity
                            WHERE datname = current_database() AND wait_event_type = 'Lock'
                            """)
                    .query(Integer.class).single();
            if (waiting == expected) {
                return;
            }
            Thread.sleep(10);
        }
        fail("Expected %d workers blocked on the post row lock, saw %d. Activity: %s",
                expected, waiting, jdbcClient.sql("""
                                SELECT pid, state, wait_event_type, wait_event, left(query, 80) AS query
                                FROM pg_stat_activity WHERE datname = current_database()
                                """)
                        .query().listOfRows());
    }

    private static <T> T await(Future<T> future, String name) throws Exception {
        try {
            return future.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (TimeoutException exception) {
            future.cancel(true);
            throw new AssertionError(name + " did not finish within " + TIMEOUT_SECONDS
                    + "s (possible deadlock or lock leak)", exception);
        }
    }

    private static void execute(Connection connection, String sql, long postId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, postId);
            statement.execute();
        }
    }

    private long fixturePost(String author, int baseLikeCount) {
        long id = insertPost("fixture", "concurrency", author, baseLikeCount, T, false);
        fixturePosts.add(id);
        return id;
    }

    private int likeRows(long postId, String userId) {
        return jdbcClient.sql("SELECT COUNT(*) FROM post_likes WHERE post_id = :postId AND user_id = :userId")
                .param("postId", postId)
                .param("userId", userId)
                .query(Integer.class)
                .single();
    }

    private int readableComments(long postId) {
        return jdbcClient.sql("SELECT COUNT(*) FROM comments WHERE post_id = :id AND is_deleted = FALSE")
                .param("id", postId).query(Integer.class).single();
    }

    // A worker's result: its value, or the exception it threw.
    private record Outcome<T>(T value, Throwable error) {

        static <T> Outcome<T> of(Callable<T> call) {
            try {
                return new Outcome<>(call.call(), null);
            } catch (Throwable error) {
                return new Outcome<>(null, error);
            }
        }

        boolean succeeded() {
            return error == null;
        }

        void assertSucceeded() {
            if (error != null) {
                throw new AssertionError("expected success", error);
            }
        }

        void assertFailedWith(HttpStatus status, String code) {
            if (!(error instanceof ApiException exception)) {
                throw new AssertionError("expected ApiException " + status, error);
            }
            assertThat(exception.status()).isEqualTo(status);
            assertThat(exception.code()).isEqualTo(code);
        }
    }
}
