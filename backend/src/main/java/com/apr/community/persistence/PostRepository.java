package com.apr.community.persistence;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import com.apr.community.common.pagination.CursorPosition;
import com.apr.community.post.PostLikeCursor;
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

    // Readable posts matching the optional search pattern (see containsPattern).
    public long countReadable(String searchPattern) {
        var statement = jdbcClient.sql("""
                        SELECT COUNT(*) FROM posts p
                        WHERE p.is_deleted = FALSE
                        """ + searchPredicate(searchPattern));
        if (searchPattern != null) {
            statement = statement.param("searchPattern", searchPattern);
        }
        return statement.query(Long.class).single();
    }

    // Case-insensitive substring match over title and content. The user's text
    // is a bound parameter; `\`, `%` and `_` are escaped so they match
    // literally instead of acting as LIKE wildcards.
    public static String containsPattern(String query) {
        if (query == null) {
            return null;
        }
        String escaped = query
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
        return "%" + escaped + "%";
    }

    private static String searchPredicate(String searchPattern) {
        return searchPattern == null
                ? ""
                : """
                        AND (p.title ILIKE :searchPattern ESCAPE '\\'
                             OR p.content ILIKE :searchPattern ESCAPE '\\')
                        """;
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

    // Newest first: ORDER BY created_at DESC, id DESC with a (created_at, id)
    // keyset. The per-row aggregates are computed only for the LIMITed rows.
    public List<ReadablePost> findReadableLatestPage(
            String requester,
            String searchPattern,
            CursorPosition cursor,
            int fetchSize) {
        // Row-value comparison is equivalent to "created_at < t OR (created_at = t AND id < i)"
        // for these NOT NULL columns, and PostgreSQL can use it as an index bound.
        String cursorPredicate = cursor == null
                ? ""
                : """
                        AND (p.created_at, p.id) < (:cursorCreatedAt, :cursorId)
                        """;
        cursorPredicate = searchPredicate(searchPattern) + cursorPredicate;

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

        if (searchPattern != null) {
            statement = statement.param("searchPattern", searchPattern);
        }
        if (cursor != null) {
            statement = statement
                    .param("cursorCreatedAt", OffsetDateTime.ofInstant(cursor.createdAt(), ZoneOffset.UTC))
                    .param("cursorId", cursor.id());
        }

        return statement.query((resultSet, rowNumber) -> readablePost(resultSet)).list();
    }

    // Most liked first: ORDER BY effective like count DESC, created_at DESC,
    // id DESC, with a (like_count, created_at, id) keyset. The effective count
    // takes part in the ordering, so it is computed for every candidate post
    // (one GROUP BY over post_likes) before the keyset and LIMIT; comment
    // counts and the requester's like are then attached to the page rows only.
    // One SQL statement; no per-post round trips.
    public List<ReadablePost> findReadableLikesPage(
            String requester,
            String searchPattern,
            PostLikeCursor cursor,
            int fetchSize) {
        String cursorPredicate = cursor == null
                ? ""
                : """
                        WHERE (ranked.like_count, ranked.created_at, ranked.id)
                            < (:cursorLikeCount, :cursorCreatedAt, :cursorId)
                        """;

        var statement = jdbcClient.sql("""
                        WITH like_counts AS (
                            SELECT post_id, COUNT(*) AS likes
                            FROM post_likes
                            GROUP BY post_id
                        ),
                        ranked AS (
                            SELECT
                                p.id,
                                p.title,
                                p.content,
                                p.author,
                                p.created_at,
                                p.base_like_count + COALESCE(lc.likes, 0) AS like_count
                            FROM posts p
                            LEFT JOIN like_counts lc ON lc.post_id = p.id
                            WHERE p.is_deleted = FALSE
                        """ + searchPredicate(searchPattern) + """
                        ),
                        page AS (
                            SELECT * FROM ranked
                        """ + cursorPredicate + """
                            ORDER BY like_count DESC, created_at DESC, id DESC
                            LIMIT :fetchSize
                        )
                        SELECT
                            page.id,
                            page.title,
                            page.content,
                            page.like_count,
                            (SELECT COUNT(*) FROM comments c
                                WHERE c.post_id = page.id AND c.is_deleted = FALSE) AS comment_count,
                            EXISTS (
                                SELECT 1 FROM post_likes requester_like
                                WHERE requester_like.post_id = page.id
                                  AND requester_like.user_id = :requester
                            ) AS is_liked,
                            page.author,
                            page.created_at
                        FROM page
                        ORDER BY page.like_count DESC, page.created_at DESC, page.id DESC
                        """)
                .param("requester", requester)
                .param("fetchSize", fetchSize);

        if (searchPattern != null) {
            statement = statement.param("searchPattern", searchPattern);
        }
        if (cursor != null) {
            statement = statement
                    .param("cursorLikeCount", cursor.likeCount())
                    .param("cursorCreatedAt", OffsetDateTime.ofInstant(cursor.createdAt(), ZoneOffset.UTC))
                    .param("cursorId", cursor.id());
        }

        return statement.query((resultSet, rowNumber) -> readablePost(resultSet)).list();
    }

    private static ReadablePost readablePost(ResultSet resultSet) throws SQLException {
        return new ReadablePost(
                resultSet.getLong("id"),
                resultSet.getString("title"),
                resultSet.getString("content"),
                resultSet.getLong("like_count"),
                resultSet.getLong("comment_count"),
                resultSet.getBoolean("is_liked"),
                resultSet.getString("author"),
                resultSet.getObject("created_at", OffsetDateTime.class).toInstant());
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
