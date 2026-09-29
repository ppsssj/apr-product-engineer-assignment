package com.apr.community.persistence;

import java.time.OffsetDateTime;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class SeedHistoryRepository {

    private final JdbcClient jdbcClient;

    public SeedHistoryRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    public boolean hasImported(String seedKey) {
        return jdbcClient.sql("SELECT EXISTS (SELECT 1 FROM seed_history WHERE seed_key = :seedKey)")
                .param("seedKey", seedKey)
                .query(Boolean.class)
                .single();
    }

    public void markImported(String seedKey, OffsetDateTime importedAt) {
        jdbcClient.sql("INSERT INTO seed_history (seed_key, imported_at) VALUES (:seedKey, :importedAt)")
                .param("seedKey", seedKey)
                .param("importedAt", importedAt)
                .update();
    }
}
