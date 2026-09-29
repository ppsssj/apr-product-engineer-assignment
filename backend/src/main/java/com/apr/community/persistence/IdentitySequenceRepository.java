package com.apr.community.persistence;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class IdentitySequenceRepository {

    private final JdbcClient jdbcClient;

    public IdentitySequenceRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    public void synchronizeWithSeededRows() {
        synchronize("posts");
        synchronize("comments");
    }

    private void synchronize(String tableName) {
        jdbcClient.sql("""
                        SELECT setval(
                            pg_get_serial_sequence(:tableName, 'id'),
                            (SELECT MAX(id) FROM %s),
                            TRUE
                        )
                        """.formatted(tableName))
                .param("tableName", tableName)
                .query(Long.class)
                .single();
    }
}
