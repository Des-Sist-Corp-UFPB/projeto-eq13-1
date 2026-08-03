package br.ufpb.dsc.jobhub.service;

import br.ufpb.dsc.jobhub.domain.Subscription;
import com.stripe.model.Event;
import com.stripe.model.checkout.Session;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class StripeWebhookServiceTest {

    private final BillingService billingService = mock(BillingService.class);
    private final AuditLogService auditLogService = mock(AuditLogService.class);

    @Test
    void requiresConfiguredSecretAndSignature() {
        StripeWebhookService withoutSecret = new StripeWebhookService(billingService, auditLogService, "");
        StripeWebhookService configured = new StripeWebhookService(billingService, auditLogService, "whsec_test");

        assertThatThrownBy(() -> withoutSecret.handle("{}", "signature"))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> configured.handle("{}", ""))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> configured.handle("{}", "t=1,v1=invalid"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void activatesSubscriptionOnlyForVerifiedCheckoutEvent() {
        StripeWebhookService service = new StripeWebhookService(billingService, auditLogService, "whsec_test");
        Event event = mock(Event.class, RETURNS_DEEP_STUBS);
        Session session = mock(Session.class);
        Subscription subscription = activeSubscription("empresa@example.com");
        when(event.getType()).thenReturn("checkout.session.completed");
        when(event.getDataObjectDeserializer().getObject()).thenReturn(Optional.of(session));
        when(session.getMetadata()).thenReturn(Map.of("subscriptionId", "42"));
        when(session.getSubscription()).thenReturn("sub_42");
        when(billingService.activateAfterConfirmedPayment(42L, "sub_42")).thenReturn(subscription);

        service.processVerifiedEvent(event);

        verify(billingService).activateAfterConfirmedPayment(42L, "sub_42");
        verify(auditLogService).logSystem("empresa@example.com", "STRIPE_SUBSCRIPTION_ACTIVATED",
                "SUBSCRIPTION", subscription.getId(), "Pagamento confirmado pelo webhook assinado do Stripe.");
    }

    @Test
    void cancelsSubscriptionFromProviderEventAndValidatesMetadata() {
        StripeWebhookService service = new StripeWebhookService(billingService, auditLogService, "whsec_test");
        Event event = mock(Event.class, RETURNS_DEEP_STUBS);
        com.stripe.model.Subscription stripeSubscription = mock(com.stripe.model.Subscription.class);
        Subscription subscription = activeSubscription("cancel@example.com");
        when(event.getType()).thenReturn("customer.subscription.deleted");
        when(event.getDataObjectDeserializer().getObject()).thenReturn(Optional.of(stripeSubscription));
        when(stripeSubscription.getMetadata()).thenReturn(Map.of("subscriptionId", "7"));
        when(stripeSubscription.getId()).thenReturn("sub_7");
        when(billingService.cancelFromProvider(7L, "sub_7")).thenReturn(subscription);

        service.processVerifiedEvent(event);

        verify(billingService).cancelFromProvider(7L, "sub_7");
        verify(auditLogService).logSystem("cancel@example.com", "STRIPE_SUBSCRIPTION_CANCELED",
                "SUBSCRIPTION", subscription.getId(), "Cancelamento confirmado pelo Stripe.");

        when(stripeSubscription.getMetadata()).thenReturn(Map.of("subscriptionId", "invalid"));
        assertThatThrownBy(() -> service.processVerifiedEvent(event))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private Subscription activeSubscription(String email) {
        Subscription subscription = new Subscription("Empresa", email, "monthly-basic");
        subscription.activate(Instant.now().plusSeconds(3600), "sub_test");
        return subscription;
    }
}
