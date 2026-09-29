package com.apr.community.seed;

import java.io.IOException;
import java.nio.file.Path;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

@Component
public class SeedDataReader {

    private final ObjectMapper objectMapper;

    public SeedDataReader(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public SeedData read(Path path) throws IOException {
        return objectMapper.readValue(path.toFile(), SeedData.class);
    }
}
