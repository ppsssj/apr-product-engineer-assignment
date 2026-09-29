package com.apr.community.persistence;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import com.apr.community.common.pagination.CursorPosition;
import com.apr.community.seed.SeedData.SeedPost;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class PostRepository {

    private final JdbcClient jdbcClient;

    public PostRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    public void insertSeedPost(SeedPost post, boolean isDeleted) {
        jdbcClient.sql("""
                        INSERT INTO posts (id, title, content, author, base_like_count, created_at, is_deleted)
                        VALUES (:id, :title, :content, :author, :baseLikeCount, :createdAt, :isDeleted)
                        """)
                .param("id", post.id())
                .param("title", post.title())
                .param("content", post.content())
                .param("author", post.author())
                .param("baseLikeCount", post.likeCount())
                .param("createdAt", post.createdAt().atOffset(ZoneOffset.UTC))
                .param("isDeleted", isDeleted)
                .update();
    }

    public long countReadable() {
        return jdbcClient.sql("SELECT COUNT(*) FROM posts WHERE is_deleted = FALSE")
                .query(Long.class)
                .single();
    }

    public boolean existsReadable(long postId) {
        return jdbcClient.sql("SELECT EXISTS (SELECT 1 FROM posts WHERE id = :postId AND is_deleted = FALSE)")
                .param("postId", postId)
                .query(Boolean.class)
                .single();
    }

    public long insertPost(String title, String content, String author, Instant createdAt) {
        return jdbcClient.sql("""
                        INSERT INTO posts (title, content, author, base_like_count, created_at, is_deleted)
                        VALUES (:title, :content, :author, 0, :createdAt, FALSE)
                        RETURNING id
                        """)
                .param("title", title)
                .param("content", content)
                .param("author", author)
                .param("createdAt", createdAt.atOffset(ZoneOffset.UTC))
                .query(Long.class)
                .single();
    }

    public Optional<PostMutationTarget> findReadableMutationTargetForUpdate(long postId) {
        return jdbcClient.sql("""
                        SELECT id, author
                        FROM posts
                        WHERE id = :postId
                          AND is_deleted = FALSE
                        FOR UPDATE
                        """)
                .param("postId", postId)
                .query((resultSet, rowNumber) -> new PostMutationTarget(
                        resultSet.getLong("id"),
                        resultSet.getString("author")))
                .optional();
    }

    public int updateReadablePost(long postId, String title, String content) {
        return jdbcClient.sql("""
                        UPDATE posts
                        SET title = :title, content = :content
                        WHERE id = :postId AND is_deleted = FALSE
                        """)
                .param("postId", postId)
                .param("title", title)
                .param("content", content)
                .update();
    }

    public int softDelete(long postId) {
        return jdbcClient.sql("""
                        UPDATE posts
                        SET is_deleted = TRUE
                        WHERE id = :postId AND is_deleted = FALSE
                        """)
                .param("postId", postId)
                .update();
    }

    public List<ReadablePost> findReadablePage(
            String requester,
            CursorPosition cursor,
            int fetchSize) {
        // Row-value comparison is equivalent to "created_at < t OR (created_at = t AND id < i)"
        // for these NOT NULL columns, and PostgreSQL can use it as an index bound.
        String cursorPredicate = cursor == null
                ? ""
                : """
                        AND (p.created_at, p.id) < (:cursorCreatedAt, :cursorId)
                        """;

        var statement = jdbcClient.sql("""
                        SELECT
                            p.id,
                            p.title,
                            p.content,
                            p.base_like_count
                                + (SELECT COUNT(*) FROM post_likes pl WHERE pl.post_id = p.id) AS like_count,
                            (SELECT COUNT(*) FROM comments c
                                WHERE c.post_id = p.id AND c.is_deleted = FALSE) AS comment_count,
                            EXISTS (
                                SELECT 1 FROM post_likes requester_like
                                WHERE requester_like.post_id = p.id
                                  AND requester_like.user_id = :requester
                            ) AS is_liked,
                            p.author,
                            p.created_at
                        FROM posts p
                        WHERE p.is_deleted = FALSE
                        """ + cursorPredicate + """
                        ORDER BY p.created_at DESC, p.id DESC
                        LIMIT :fetchSize
                        """)
                .param("requester", requester)
                .param("fetchSize", fetchSize);

        if (cursor != null) {
            statement = statement
                    .param("cursorCreatedAt", OffsetDateTime.ofInstant(cursor.createdAt(), ZoneOffset.UTC))
                    .param("cursorId", cursor.id());
        }

        return statement.query((resultSet, rowNumber) -> new ReadablePost(
                        resultSet.getLong("id"),
                        resultSet.getString("title"),
                        resultSet.getString("content"),
                        resultSet.getLong("like_count"),
                        resultSet.getLong("comment_count"),
                        resultSet.getBoolean("is_liked"),
                        resultSet.getString("author"),
                        resultSet.getObject("created_at", OffsetDateTime.class).toInstant()))
                .list();
    }

    public Optional<ReadablePost> findReadableById(long postId, String requester) {
        return jdbcClient.sql("""
                        SELECT
                            p.id,
                            p.title,
                            p.content,
                            p.base_like_count
                                + (SELECT COUNT(*) FROM post_likes pl WHERE pl.post_id = p.id) AS like_count,
                            (SELECT COUNT(*) FROM comments c
                                WHERE c.post_id = p.id AND c.is_deleted = FALSE) AS comment_count,
                            EXISTS (
                                SELECT 1 FROM post_likes requester_like
                                WHERE requester_like.post_id = p.id
                                  AND requester_like.user_id = :requester
                            ) AS is_liked,
                            p.author,
                            p.created_at
                        FROM posts p
                        WHERE p.id = :postId
                          AND p.is_deleted = FALSE
                        """)
                .param("postId", postId)
                .param("requester", requester)
                .query((resultSet, rowNumber) -> new ReadablePost(
                        resultSet.getLong("id"),
                        resultSet.getString("title"),
                        resultSet.getString("content"),
                        resultSet.getLong("like_count"),
                        resultSet.getLong("comment_count"),
                        resultSet.getBoolean("is_liked"),
                        resultSet.getString("author"),
                        resultSet.getObject("created_at", OffsetDateTime.class).toInstant()))
                .optional();
    }

    public int countAll() {
        return jdbcClient.sql("SELECT COUNT(*) FROM posts")
                .query(Integer.class)
                .single();
    }

    public record ReadablePost(
            long id,
            String title,
            String content,
            long likeCount,
            long commentCount,
            boolean isLiked,
            String author,
            Instant createdAt) {
    }

    public record PostMutationTarget(long id, String author) {
    }
}
