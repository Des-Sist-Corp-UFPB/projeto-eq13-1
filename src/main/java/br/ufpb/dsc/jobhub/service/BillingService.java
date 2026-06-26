package br.ufpb.dsc.jobhub.service;

import br.ufpb.dsc.jobhub.domain.Subscription;
import br.ufpb.dsc.jobhub.domain.SubscriptionStatus;
import br.ufpb.dsc.jobhub.repository.SubscriptionRepository;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Objects;

@Service
public class BillingService {

    private static final String MONTHLY_PLAN_CODE = "monthly-basic";

    private final SubscriptionRepository subscriptionRepository;
    private final String secretKey;
    private final String priceId;
    private final String successUrl;
    private final String cancelUrl;

    public BillingService(SubscriptionRepository subscriptionRepository,
                          @Value("${stripe.secret-key:}") String secretKey,
                          @Value("${stripe.monthly-price-id:}") String priceId,
                          @Value("${stripe.success-url:}") String successUrl,
                          @Value("${stripe.cancel-url:}") String cancelUrl) {
        this.subscriptionRepository = subscriptionRepository;
        this.secretKey = secretKey;
        this.priceId = priceId;
        this.successUrl = successUrl;
        this.cancelUrl = cancelUrl;
    }

    @Transactional(readOnly = true)
    public boolean hasActiveSubscription(String companyEmail) {
        return subscriptionRepository.findByCompanyEmailIgnoreCase(companyEmail)
                .filter(Subscription::isActive)
                .isPresent();
    }

    @Transactional(readOnly = true)
    public Subscription findByCompanyEmail(String companyEmail) {
        return subscriptionRepository.findByCompanyEmailIgnoreCase(companyEmail)
                .orElse(null);
    }

    @Transactional
    public Subscription startMonthlySubscription(String company, String companyEmail) {
        Subscription subscription = subscriptionRepository.findByCompanyEmailIgnoreCase(companyEmail)
                .orElseGet(() -> new Subscription(company, companyEmail, MONTHLY_PLAN_CODE));
        if (!MONTHLY_PLAN_CODE.equals(subscription.getPlanCode())) {
            subscription = new Subscription(company, companyEmail, MONTHLY_PLAN_CODE);
        }
        subscription.activate(Instant.now().plusSeconds(30L * 24 * 60 * 60), null);
        return subscriptionRepository.save(subscription);
    }

    @Transactional
    public Subscription markExpired(String companyEmail) {
        Subscription subscription = subscriptionRepository.findByCompanyEmailIgnoreCase(companyEmail)
                .orElseThrow(() -> new IllegalArgumentException("Assinatura não encontrada"));
        subscription.cancel();
        return subscriptionRepository.save(subscription);
    }

    @Transactional(readOnly = true)
    public String requiredPlanCode() {
        return MONTHLY_PLAN_CODE;
    }

    @Transactional
    public CheckoutResult createCheckoutSession(String company, String companyEmail) {
        if (secretKey == null || secretKey.isBlank()) {
            throw new IllegalStateException("Configuração do Stripe ausente");
        }
        if (priceId == null || priceId.isBlank()) {
            throw new IllegalStateException("Stripe monthly price id não configurado");
        }
        Stripe.apiKey = secretKey;

        Subscription subscription = subscriptionRepository.findByCompanyEmailIgnoreCase(companyEmail)
                .orElseGet(() -> new Subscription(company, companyEmail, MONTHLY_PLAN_CODE));
        if (!Objects.equals(subscription.getCompany(), company)) {
            subscription = new Subscription(company, companyEmail, MONTHLY_PLAN_CODE);
        }
        subscription.activate(Instant.now().plusSeconds(30L * 24 * 60 * 60), null);
        subscription = subscriptionRepository.save(subscription);

        try {
            SessionCreateParams params = SessionCreateParams.builder()
                    .setMode(SessionCreateParams.Mode.SUBSCRIPTION)
                    .addLineItem(SessionCreateParams.LineItem.builder()
                            .setQuantity(1L)
                            .setPrice(priceId)
                            .build())
                    .setSuccessUrl(successUrl + "?subscription=" + subscription.getId())
                    .setCancelUrl(cancelUrl + "?subscription=" + subscription.getId())
                    .putMetadata("subscriptionId", String.valueOf(subscription.getId()))
                    .putMetadata("companyEmail", companyEmail)
                    .build();
            Session session = Session.create(params);
            subscription.activate(subscription.getValidUntil(), session.getId());
            subscriptionRepository.save(subscription);
            return new CheckoutResult(subscription, session.getUrl());
        } catch (StripeException ex) {
            throw new IllegalStateException("Não foi possível iniciar o checkout do Stripe", ex);
        }
    }

    public record CheckoutResult(Subscription subscription, String checkoutUrl) {
    }
}