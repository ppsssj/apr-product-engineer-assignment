package com.apr.community.seed;

import java.io.IOException;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HashSet;

import com.apr.community.persistence.CommentRepository;
import com.apr.community.persistence.IdentitySequenceRepository;
import com.apr.community.persistence.PostRepository;
import com.apr.community.persistence.SeedHistoryRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class SeedInitializer implements ApplicationRunner {

    static final String SEED_KEY = "assignment-v1";

    private final SeedDataReader seedDataReader;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final IdentitySequenceRepository identitySequenceRepository;
    private final SeedHistoryRepository seedHistoryRepository;
    private final SeedReadiness seedReadiness;
    private final Path seedPath;

    public SeedInitializer(
            SeedDataReader seedDataReader,
            PostRepository postRepository,
            CommentRepository commentRepository,
            IdentitySequenceRepository identitySequenceRepository,
            SeedHistoryRepository seedHistoryRepository,
            SeedReadiness seedReadiness,
            @Value("${app.seed-path}") String seedPath) {
        this.seedDataReader = seedDataReader;
        this.postRepository = postRepository;
        this.commentRepository = commentRepository;
        this.identitySequenceRepository = identitySequenceRepository;
        this.seedHistoryRepository = seedHistoryRepository;
        this.seedReadiness = seedReadiness;
        this.seedPath = Path.of(seedPath);
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) throws IOException {
        if (seedHistoryRepository.hasImported(SEED_KEY)) {
            seedReadiness.markReadyAfterCommit();
            return;
        }

        SeedData seedData = seedDataReader.read(seedPath);
        var deletedPostIds = new HashSet<>(seedData.deletedPostIds());
        var deletedCommentIds = new HashSet<>(seedData.deletedCommentIds());

        for (SeedData.SeedPost post : seedData.posts()) {
            postRepository.insertSeedPost(post, deletedPostIds.contains(post.id()));
        }
        for (SeedData.SeedComment comment : seedData.comments()) {
            commentRepository.insertSeedComment(comment, deletedCommentIds.contains(comment.id()));
        }

        identitySequenceRepository.synchronizeWithSeededRows();
        seedHistoryRepository.markImported(SEED_KEY, OffsetDateTime.now(ZoneOffset.UTC));
        seedReadiness.markReadyAfterCommit();
    }
}
