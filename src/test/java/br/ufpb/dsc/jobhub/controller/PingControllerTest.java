package br.ufpb.dsc.jobhub.controller;

import br.ufpb.dsc.jobhub.service.DatabaseHealthService;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PingControllerTest {

    private final DatabaseHealthService databaseHealthService = mock(DatabaseHealthService.class);
    private final PingController controller = new PingController(databaseHealthService);

    @Test
    void returnsServiceUnavailableWhenDatabaseIsDownOrThrows() {
        when(databaseHealthService.isDatabaseAvailable()).thenReturn(false);
        assertThat(controller.ping().getStatusCode().value()).isEqualTo(503);
        assertThat(controller.ping().getBody()).containsEntry("database", "down");

        when(databaseHealthService.isDatabaseAvailable()).thenThrow(new IllegalStateException("database down"));
        assertThat(controller.ping().getStatusCode().value()).isEqualTo(503);
    }
}
