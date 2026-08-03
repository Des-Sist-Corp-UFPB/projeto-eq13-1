package br.ufpb.dsc.jobhub.controller;

import br.ufpb.dsc.jobhub.service.StripeWebhookService;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class StripeWebhookControllerTest {

    private final StripeWebhookService service = mock(StripeWebhookService.class);
    private final StripeWebhookController controller = new StripeWebhookController(service);

    @Test
    void returnsProcessedOnlyAfterWebhookValidation() {
        var response = controller.receive("{}", "valid-signature");

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).containsEntry("status", "processed");
        verify(service).handle("{}", "valid-signature");
    }

    @Test
    void distinguishesInvalidRequestFromMissingServerConfiguration() {
        doThrow(new IllegalArgumentException("invalid")).when(service).handle("invalid", "signature");
        doThrow(new IllegalStateException("missing")).when(service).handle("{}", null);

        var invalid = controller.receive("invalid", "signature");
        var unavailable = controller.receive("{}", null);

        assertThat(invalid.getStatusCode().value()).isEqualTo(400);
        assertThat(invalid.getBody()).containsEntry("status", "invalid");
        assertThat(unavailable.getStatusCode().value()).isEqualTo(503);
        assertThat(unavailable.getBody()).containsEntry("status", "not_configured");
    }
}
