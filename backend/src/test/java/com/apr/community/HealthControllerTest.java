package com.apr.community;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

import javax.sql.DataSource;

import com.apr.community.seed.SeedReadiness;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class HealthControllerTest {

    @Test
    void staysUnavailableUntilSeedReadinessHasCommitted() {
        var dataSource = mock(DataSource.class);
        var controller = new HealthController(dataSource, new SeedReadiness());

        var response = controller.health();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(response.getBody()).containsEntry("status", "starting");
        verifyNoInteractions(dataSource);
    }
}
