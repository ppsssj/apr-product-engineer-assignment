package com.apr.community.seed;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

import com.apr.community.persistence.CommentRepository;
import com.apr.community.persistence.PostRepository;
import com.apr.community.support.PostgresIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

class SeedIntegrationTest extends PostgresIntegrationTest {

    @Autowired
    private SeedInitializer seedInitializer;

    @Autowired
    private SeedDataReader seedDataReader;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private CommentRepository commentRepository;

    @Value("${app.seed-path}")
    private String seedPath;

    @Test
    void startupImportedTheSeedExactlyOnce() throws Exception {
        assertThat(Path.of(seedPath)).isAbsolute().exists();
        SeedData seed = seedDataReader.read(Path.of(seedPath));

        assertThat(seedHistoryCount()).isEqualTo(1);
        assertThat(postRepository.countAll()).isEqualTo(seed.posts().size());
        assertThat(commentRepository.countAll()).isEqualTo(seed.comments().size());
        assertThat(idsWhereDeleted("posts")).containsExactlyInAnyOrderElementsOf(seed.deletedPostIds());
        assertThat(idsWhereDeleted("comments")).containsExactlyInAnyOrderElementsOf(seed.deletedCommentIds());
    }

    @Test
    void rerunningTheInitializerDoesNotInsertAgain() throws Exception {
        int posts = postRepository.countAll();
        int comments = commentRepository.countAll();

        seedInitializer.run(null);

        assertThat(seedHistoryCount()).isEqualTo(1);
        assertThat(postRepository.countAll()).isEqualTo(posts);
        assertThat(commentRepository.countAll()).isEqualTo(comments);
    }

    @Test
    void identitySequencesAreAheadOfSeededIds() {
        // The sequence may already be past MAX(id): earlier tests' inserts were
        // rolled back, but sequence increments are not.
        assertThat(sequenceLastValue("posts")).isGreaterThanOrEqualTo(maxId("posts"));
        assertThat(sequenceLastValue("comments")).isGreaterThanOrEqualTo(maxId("comments"));

        long maxPostId = maxId("posts");
        long maxCommentId = maxId("comments");
        long postId = postRepository.insertPost("t", "c", "u", Instant.now());
        long commentId = commentRepository.insertComment(postId, "c", "u", Instant.now());

        assertThat(postId).isGreaterThan(maxPostId);
        assertThat(commentId).isGreaterThan(maxCommentId);
    }

    private int seedHistoryCount() {
        return jdbcClient.sql("SELECT COUNT(*) FROM seed_history WHERE seed_key = :key")
                .param("key", SeedInitializer.SEED_KEY)
                .query(Integer.class)
                .single();
    }

    private List<Long> idsWhereDeleted(String table) {
        return jdbcClient.sql("SELECT id FROM " + table + " WHERE is_deleted").query(Long.class).list();
    }

    private long maxId(String table) {
        return jdbcClient.sql("SELECT MAX(id) FROM " + table).query(Long.class).single();
    }

    private long sequenceLastValue(String table) {
        return jdbcClient.sql("SELECT pg_sequence_last_value(pg_get_serial_sequence(:table, 'id')::regclass)")
                .param("table", table)
                .query(Long.class)
                .single();
    }
}
