package com.apr.community.persistence;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

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

    public List<ReadableComment> findReadablePage(
            long postId,
            CursorPosition cursor,
            int fetchSize) {
        String cursorPredicate = cursor == null
                ? ""
                : """
                        AND (
                            c.created_at < :cursorCreatedAt
                            OR (c.created_at = :cursorCreatedAt AND c.id < :cursorId)
                        )
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
}
