package com.apr.community.persistence;

import java.time.ZoneOffset;

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

    public int countReadable() {
        return jdbcClient.sql("SELECT COUNT(*) FROM posts WHERE is_deleted = FALSE")
                .query(Integer.class)
                .single();
    }

    public int countAll() {
        return jdbcClient.sql("SELECT COUNT(*) FROM posts")
                .query(Integer.class)
                .single();
    }
}
