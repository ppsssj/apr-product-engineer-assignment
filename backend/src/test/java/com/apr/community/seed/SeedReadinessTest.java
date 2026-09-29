package com.apr.community.seed;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

class SeedReadinessTest {

    @AfterEach
    void clearSynchronization() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void becomesReadyOnlyAfterTheSeedTransactionCommits() {
        var readiness = new SeedReadiness();
        TransactionSynchronizationManager.initSynchronization();

        readiness.markReadyAfterCommit();

        assertThat(readiness.isReady()).isFalse();
        TransactionSynchronizationManager.getSynchronizations()
                .forEach(TransactionSynchronization::afterCommit);
        assertThat(readiness.isReady()).isTrue();
    }
}
