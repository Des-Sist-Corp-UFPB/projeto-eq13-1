package br.ufpb.dsc.jobhub.service;

import br.ufpb.dsc.jobhub.domain.Subscription;
import br.ufpb.dsc.jobhub.domain.SubscriptionStatus;
import br.ufpb.dsc.jobhub.repository.SubscriptionRepository;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BillingServiceTest {

    private final SubscriptionRepository subscriptionRepository = mock(SubscriptionRepository.class);

    @Test
    void checksActiveSubscriptionsAndFindsByCompanyEmail() {
        Subscription subscription = new Subscription("Empresa PB", "billing@example.com", "monthly-basic");
        subscription.activate(Instant.now().plusSeconds(3600), "cs_test_active");
        BillingService service = serviceWithStripeConfig();

        when(subscriptionRepository.findByCompanyEmailIgnoreCase("billing@example.com"))
                .thenReturn(Optional.of(subscription));

        assertThat(service.hasActiveSubscription("billing@example.com")).isTrue();
        assertThat(service.findByCompanyEmail("billing@example.com")).isSameAs(subscription);
    }

    @Test
    void expiredOrMissingSubscriptionsAreNotActive() {
        Subscription expired = new Subscription("Empresa PB", "expired@example.com", "monthly-basic");
        expired.activate(Instant.now().minusSeconds(60), "cs_test_expired");
        BillingService service = serviceWithStripeConfig();

        when(subscriptionRepository.findByCompanyEmailIgnoreCase("expired@example.com"))
                .thenReturn(Optional.of(expired));
        when(subscriptionRepository.findByCompanyEmailIgnoreCase("missing@example.com"))
                .thenReturn(Optional.empty());

        assertThat(service.hasActiveSubscription("expired@example.com")).isFalse();
        assertThat(service.hasActiveSubscription("missing@example.com")).isFalse();
        assertThat(service.findByCompanyEmail("missing@example.com")).isNull();
    }

    @Test
    void startsMonthlySubscriptionAndReplacesLegacyPlan() {
        Subscription legacy = new Subscription("Empresa Antiga", "legacy@example.com", "legacy-plan");
        BillingService service = serviceWithStripeConfig();

        when(subscriptionRepository.findByCompanyEmailIgnoreCase("legacy@example.com"))
                .thenReturn(Optional.of(legacy));
        when(subscriptionRepository.save(any(Subscription.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Subscription subscription = service.startMonthlySubscription("Empresa Nova", "legacy@example.com");

        assertThat(service.requiredPlanCode()).isEqualTo("monthly-basic");
        assertThat(subscription.getCompany()).isEqualTo("Empresa Nova");
        assertThat(subscription.getCompanyEmail()).isEqualTo("legacy@example.com");
        assertThat(subscription.getPlanCode()).isEqualTo("monthly-basic");
        assertThat(subscription.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
        assertThat(subscription.getValidUntil()).isAfter(Instant.now());
        verify(subscriptionRepository).save(subscription);
    }

    @Test
    void cancelsExistingSubscriptionAndRejectsMissingOne() {
        Subscription subscription = new Subscription("Empresa PB", "cancel@example.com", "monthly-basic");
        subscription.activate(Instant.now().plusSeconds(3600), "cs_test_cancel");
        BillingService service = serviceWithStripeConfig();

        when(subscriptionRepository.findByCompanyEmailIgnoreCase("cancel@example.com"))
                .thenReturn(Optional.of(subscription));
        when(subscriptionRepository.findByCompanyEmailIgnoreCase("missing@example.com"))
                .thenReturn(Optional.empty());
        when(subscriptionRepository.save(any(Subscription.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Subscription canceled = service.markExpired("cancel@example.com");

        assertThat(canceled.getStatus()).isEqualTo(SubscriptionStatus.CANCELED);
        assertThatThrownBy(() -> service.markExpired("missing@example.com"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Assinatura");
    }

    @Test
    void validatesStripeConfigurationBeforeCheckout() {
        BillingService withoutSecret = new BillingService(
                subscriptionRepository, "", "price_123", "http://localhost/sucesso", "http://localhost/cancelado"
        );
        BillingService withoutPrice = new BillingService(
                subscriptionRepository, "test-stripe-secret", "", "http://localhost/sucesso", "http://localhost/cancelado"
        );

        assertThatThrownBy(() -> withoutSecret.createCheckoutSession("Empresa", "billing@example.com"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Stripe");
        assertThatThrownBy(() -> withoutPrice.createCheckoutSession("Empresa", "billing@example.com"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("price id");
    }

    @Test
    void createsStripeCheckoutSessionAndStoresExternalReference() throws Exception {
        Subscription existing = new Subscription("Empresa Antiga", "checkout@example.com", "monthly-basic");
        Session stripeSession = mock(Session.class);
        BillingService service = serviceWithStripeConfig();

        when(subscriptionRepository.findByCompanyEmailIgnoreCase("checkout@example.com"))
                .thenReturn(Optional.of(existing));
        when(subscriptionRepository.save(any(Subscription.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(stripeSession.getUrl()).thenReturn("https://checkout.stripe.test/session");
        when(stripeSession.getId()).thenReturn("cs_test_123");

        try (MockedStatic<Session> sessions = mockStatic(Session.class)) {
            sessions.when(() -> Session.create(any(SessionCreateParams.class))).thenReturn(stripeSession);

            BillingService.CheckoutResult result = service.createCheckoutSession("Empresa Nova", "checkout@example.com");

            assertThat(result.checkoutUrl()).isEqualTo("https://checkout.stripe.test/session");
            assertThat(result.subscription().getCompany()).isEqualTo("Empresa Nova");
            assertThat(result.subscription().getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
            assertThat(result.subscription().getExternalReference()).isEqualTo("cs_test_123");
            sessions.verify(() -> Session.create(any(SessionCreateParams.class)));
        }
    }

    @Test
    void startsMonthlySubscriptionCreatesNewSubscriptionWhenNotFound() {
        BillingService service = serviceWithStripeConfig();

        when(subscriptionRepository.findByCompanyEmailIgnoreCase("new_company@example.com"))
                .thenReturn(Optional.empty());
        when(subscriptionRepository.save(any(Subscription.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Subscription subscription = service.startMonthlySubscription("Nova Empresa", "new_company@example.com");

        assertThat(subscription.getCompany()).isEqualTo("Nova Empresa");
        assertThat(subscription.getCompanyEmail()).isEqualTo("new_company@example.com");
        assertThat(subscription.getPlanCode()).isEqualTo("monthly-basic");
        assertThat(subscription.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
        verify(subscriptionRepository).save(subscription);
    }

    @Test
    void createCheckoutSessionCreatesNewSubscriptionWhenNotFound() throws Exception {
        Session stripeSession = mock(Session.class);
        BillingService service = serviceWithStripeConfig();

        when(subscriptionRepository.findByCompanyEmailIgnoreCase("new_checkout@example.com"))
                .thenReturn(Optional.empty());
        when(subscriptionRepository.save(any(Subscription.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(stripeSession.getUrl()).thenReturn("https://checkout.stripe.test/session");
        when(stripeSession.getId()).thenReturn("cs_test_new");

        try (MockedStatic<Session> sessions = mockStatic(Session.class)) {
            sessions.when(() -> Session.create(any(SessionCreateParams.class))).thenReturn(stripeSession);

            BillingService.CheckoutResult result = service.createCheckoutSession("Nova Empresa 2", "new_checkout@example.com");

            assertThat(result.checkoutUrl()).isEqualTo("https://checkout.stripe.test/session");
            assertThat(result.subscription().getCompany()).isEqualTo("Nova Empresa 2");
            assertThat(result.subscription().getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
            assertThat(result.subscription().getExternalReference()).isEqualTo("cs_test_new");
            sessions.verify(() -> Session.create(any(SessionCreateParams.class)));
        }
    }

    @Test
    void createCheckoutSessionPropagatesStripeExceptionAsIllegalStateException() throws Exception {
        BillingService service = serviceWithStripeConfig();

        when(subscriptionRepository.findByCompanyEmailIgnoreCase("error_checkout@example.com"))
                .thenReturn(Optional.empty());
        when(subscriptionRepository.save(any(Subscription.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        try (MockedStatic<Session> sessions = mockStatic(Session.class)) {
            sessions.when(() -> Session.create(any(SessionCreateParams.class)))
                    .thenThrow(new com.stripe.exception.ApiConnectionException("Stripe error"));

            assertThatThrownBy(() -> service.createCheckoutSession("Empresa Erro", "error_checkout@example.com"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Não foi possível iniciar o checkout do Stripe");
        }
    }

    private BillingService serviceWithStripeConfig() {
        return new BillingService(
                subscriptionRepository,
                "test-stripe-secret",
                "price_123",
                "http://localhost:8080/admin/billing/sucesso",
                "http://localhost:8080/admin/billing/cancelado"
        );
    }
}
