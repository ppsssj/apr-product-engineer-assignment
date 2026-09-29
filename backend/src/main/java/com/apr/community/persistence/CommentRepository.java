package com.apr.community.persistence;

import java.time.ZoneOffset;

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

    public int countReadableForPost(long postId) {
        return jdbcClient.sql("SELECT COUNT(*) FROM comments WHERE post_id = :postId AND is_deleted = FALSE")
                .param("postId", postId)
                .query(Integer.class)
                .single();
    }

    public int countAll() {
        return jdbcClient.sql("SELECT COUNT(*) FROM comments")
                .query(Integer.class)
                .single();
    }
}
