package com.apr.community.persistence;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class PostLikeRepository {

    private final JdbcClient jdbcClient;

    public PostLikeRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    public int countAll() {
        return jdbcClient.sql("SELECT COUNT(*) FROM post_likes")
                .query(Integer.class)
                .single();
    }

    public boolean exists(long postId, String userId) {
        return jdbcClient.sql("""
                        SELECT EXISTS (
                            SELECT 1 FROM post_likes
                            WHERE post_id = :postId AND user_id = :userId
                        )
                        """)
                .param("postId", postId)
                .param("userId", userId)
                .query(Boolean.class)
                .single();
    }

    public int insert(long postId, String userId) {
        return jdbcClient.sql("""
                        INSERT INTO post_likes (post_id, user_id)
                        VALUES (:postId, :userId)
                        """)
                .param("postId", postId)
                .param("userId", userId)
                .update();
    }

    public int delete(long postId, String userId) {
        return jdbcClient.sql("""
                        DELETE FROM post_likes
                        WHERE post_id = :postId AND user_id = :userId
                        """)
                .param("postId", postId)
                .param("userId", userId)
                .update();
    }
}
