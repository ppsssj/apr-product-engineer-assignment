package com.apr.community.seed;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;

class SeedDataReaderTest {

    @Test
    void readsTheProvidedSeedDataWithItsExpectedShape() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        SeedData seedData = new SeedDataReader(objectMapper).read(Path.of("..", "seed", "seed.json"));

        assertThat(seedData.posts()).hasSize(42);
        assertThat(seedData.comments()).hasSize(2_076);
        assertThat(seedData.deletedPostIds()).containsExactlyInAnyOrder(9L, 30L, 31L);
        assertThat(seedData.deletedCommentIds()).hasSize(81);
        assertThat(seedData.posts()).extracting(SeedData.SeedPost::id).doesNotHaveDuplicates();
        assertThat(seedData.comments()).extracting(SeedData.SeedComment::id).doesNotHaveDuplicates();
    }
}
