package com.apr.community.persistence;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import com.apr.community.common.pagination.CursorPosition;
import com.apr.community.seed.SeedData.SeedComment;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class CommentRepository {

    private final JdbcClient jdbcClient;

    public CommentRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    public void insertSeedComment(SeedComment comment, boolean isDeleted) {
        jdbcClient.sql("""
                        INSERT INTO comments (id, post_id, content, author, created_at, is_deleted)
                        VALUES (:id, :postId, :content, :author, :createdAt, :isDeleted)
                        """)
                .param("id", comment.id())
                .param("postId", comment.postId())
                .param("content", comment.content())
                .param("author", comment.author())
                .param("createdAt", comment.createdAt().atOffset(ZoneOffset.UTC))
                .param("isDeleted", isDeleted)
                .update();
    }

    public long countReadableForPost(long postId) {
        return jdbcClient.sql("SELECT COUNT(*) FROM comments WHERE post_id = :postId AND is_deleted = FALSE")
                .param("postId", postId)
                .query(Long.class)
                .single();
    }

    public long insertComment(long postId, String content, String author, Instant createdAt) {
        return jdbcClient.sql("""
                        INSERT INTO comments (post_id, content, author, created_at, is_deleted)
                        VALUES (:postId, :content, :author, :createdAt, FALSE)
                        RETURNING id
                        """)
                .param("postId", postId)
                .param("content", content)
                .param("author", author)
                .param("createdAt", createdAt.atOffset(ZoneOffset.UTC))
                .query(Long.class)
                .single();
    }

    public Optional<CommentMutationTarget> findReadableMutationTargetForUpdate(long commentId) {
        return jdbcClient.sql("""
                        SELECT c.id, c.post_id, c.author
                        FROM comments c
                        JOIN posts p ON p.id = c.post_id
                        WHERE c.id = :commentId
                          AND c.is_deleted = FALSE
                          AND p.is_deleted = FALSE
                        FOR UPDATE OF p, c
                        """)
                .param("commentId", commentId)
                .query((resultSet, rowNumber) -> new CommentMutationTarget(
                        resultSet.getLong("id"),
                        resultSet.getLong("post_id"),
                        resultSet.getString("author")))
                .optional();
    }

    public int softDelete(long commentId) {
        return jdbcClient.sql("""
                        UPDATE comments
                        SET is_deleted = TRUE
                        WHERE id = :commentId AND is_deleted = FALSE
                        """)
                .param("commentId", commentId)
                .update();
    }

    public List<ReadableComment> findReadablePage(
            long postId,
            CursorPosition cursor,
            int fetchSize) {
        // Row-value comparison is equivalent to "created_at < t OR (created_at = t AND id < i)"
        // for these NOT NULL columns, and PostgreSQL can use it as an index bound.
        String cursorPredicate = cursor == null
                ? ""
                : """
                        AND (c.created_at, c.id) < (:cursorCreatedAt, :cursorId)
                        """;

        var statement = jdbcClient.sql("""
                        SELECT c.id, c.post_id, c.content, c.author, c.created_at
                        FROM comments c
                        WHERE c.post_id = :postId
                          AND c.is_deleted = FALSE
                        """ + cursorPredicate + """
                        ORDER BY c.created_at DESC, c.id DESC
                        LIMIT :fetchSize
                        """)
                .param("postId", postId)
                .param("fetchSize", fetchSize);

        if (cursor != null) {
            statement = statement
                    .param("cursorCreatedAt", OffsetDateTime.ofInstant(cursor.createdAt(), ZoneOffset.UTC))
                    .param("cursorId", cursor.id());
        }

        return statement.query((resultSet, rowNumber) -> new ReadableComment(
                        resultSet.getLong("id"),
                        resultSet.getLong("post_id"),
                        resultSet.getString("content"),
                        resultSet.getString("author"),
                        resultSet.getObject("created_at", OffsetDateTime.class).toInstant()))
                .list();
    }

    public int countAll() {
        return jdbcClient.sql("SELECT COUNT(*) FROM comments")
                .query(Integer.class)
                .single();
    }

    public record ReadableComment(
            long id,
            long postId,
            String content,
            String author,
            Instant createdAt) {
    }

    public record CommentMutationTarget(long id, long postId, String author) {
    }
}
