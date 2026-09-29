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
}
