package com.apr.community.support;

import java.time.Instant;
import java.time.ZoneOffset;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;

// Boots the full application (Flyway V1 + SeedInitializer) against a
// disposable PostgreSQL container. The container is started once per test JVM
// and shared by every subclass, so they all reuse one cached Spring context.
// Each test runs in a transaction that is rolled back; identity sequences are
// not rolled back, so tests must not depend on exact generated ids.
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Transactional
public abstract class PostgresIntegrationTest {

    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    static {
        POSTGRES.start();
    }

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired
    protected JdbcClient jdbcClient;

    // Fixture insert bypassing the service layer, for states the API cannot
    // produce directly (explicit base like count, timestamp or deleted flag).
    protected long insertPost(
            String title,
            String content,
            String author,
            int baseLikeCount,
            Instant createdAt,
            boolean isDeleted) {
        return jdbcClient.sql("""
                        INSERT INTO posts (title, content, author, base_like_count, created_at, is_deleted)
                        VALUES (:title, :content, :author, :baseLikeCount, :createdAt, :isDeleted)
                        RETURNING id
                        """)
                .param("title", title)
                .param("content", content)
                .param("author", author)
                .param("baseLikeCount", baseLikeCount)
                .param("createdAt", createdAt.atOffset(ZoneOffset.UTC))
                .param("isDeleted", isDeleted)
                .query(Long.class)
                .single();
    }

    protected long insertComment(long postId, String content, String author, Instant createdAt, boolean isDeleted) {
        return jdbcClient.sql("""
                        INSERT INTO comments (post_id, content, author, created_at, is_deleted)
                        VALUES (:postId, :content, :author, :createdAt, :isDeleted)
                        RETURNING id
                        """)
                .param("postId", postId)
                .param("content", content)
                .param("author", author)
                .param("createdAt", createdAt.atOffset(ZoneOffset.UTC))
                .param("isDeleted", isDeleted)
                .query(Long.class)
                .single();
    }

    protected void insertLike(long postId, String userId) {
        jdbcClient.sql("INSERT INTO post_likes (post_id, user_id) VALUES (:postId, :userId)")
                .param("postId", postId)
                .param("userId", userId)
                .update();
    }

    protected boolean isDeletedPost(long postId) {
        return jdbcClient.sql("SELECT is_deleted FROM posts WHERE id = :id")
                .param("id", postId)
                .query(Boolean.class)
                .single();
    }
}
