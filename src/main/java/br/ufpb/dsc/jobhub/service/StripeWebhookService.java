package br.ufpb.dsc.jobhub.service;

import br.ufpb.dsc.jobhub.domain.Subscription;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.StripeObject;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class StripeWebhookService {

    private final BillingService billingService;
    private final AuditLogService auditLogService;
    private final String webhookSecret;

    public StripeWebhookService(BillingService billingService,
                                AuditLogService auditLogService,
                                @Value("${stripe.webhook-secret:}") String webhookSecret) {
        this.billingService = billingService;
        this.auditLogService = auditLogService;
        this.webhookSecret = webhookSecret;
    }

    public void handle(String payload, String signatureHeader) {
        if (webhookSecret == null || webhookSecret.isBlank()) {
            throw new IllegalStateException("Webhook do Stripe não configurado");
        }
        if (signatureHeader == null || signatureHeader.isBlank()) {
            throw new IllegalArgumentException("Assinatura do webhook ausente");
        }
        try {
            processVerifiedEvent(Webhook.constructEvent(payload, signatureHeader, webhookSecret));
        } catch (SignatureVerificationException ex) {
            throw new IllegalArgumentException("Assinatura do webhook inválida", ex);
        }
    }

    public void processVerifiedEvent(Event event) {
        StripeObject object = event.getDataObjectDeserializer().getObject().orElse(null);
        if ("checkout.session.completed".equals(event.getType()) && object instanceof Session session) {
            Long subscriptionId = metadataId(session.getMetadata());
            if (subscriptionId == null) {
                throw new IllegalArgumentException("Webhook sem identificador da assinatura");
            }
            String externalReference = session.getSubscription() == null
                    ? session.getId()
                    : session.getSubscription();
            Subscription subscription = billingService.activateAfterConfirmedPayment(subscriptionId, externalReference);
            auditLogService.logSystem(subscription.getCompanyEmail(), "STRIPE_SUBSCRIPTION_ACTIVATED",
                    "SUBSCRIPTION", subscription.getId(), "Pagamento confirmado pelo webhook assinado do Stripe.");
            return;
        }
        if ("customer.subscription.deleted".equals(event.getType())
                && object instanceof com.stripe.model.Subscription stripeSubscription) {
            Long subscriptionId = metadataId(stripeSubscription.getMetadata());
            Subscription subscription = billingService.cancelFromProvider(subscriptionId, stripeSubscription.getId());
            auditLogService.logSystem(subscription.getCompanyEmail(), "STRIPE_SUBSCRIPTION_CANCELED",
                    "SUBSCRIPTION", subscription.getId(), "Cancelamento confirmado pelo Stripe.");
        }
    }

    private Long metadataId(Map<String, String> metadata) {
        if (metadata == null || metadata.get("subscriptionId") == null) {
            return null;
        }
        try {
            return Long.valueOf(metadata.get("subscriptionId"));
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Identificador de assinatura inválido", ex);
        }
    }
}
