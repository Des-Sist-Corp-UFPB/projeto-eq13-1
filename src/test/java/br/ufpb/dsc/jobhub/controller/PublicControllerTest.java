package br.ufpb.dsc.jobhub.controller;

import br.ufpb.dsc.jobhub.domain.AppUser;
import br.ufpb.dsc.jobhub.domain.AuthProvider;
import br.ufpb.dsc.jobhub.domain.ContractType;
import br.ufpb.dsc.jobhub.domain.JobLocationType;
import br.ufpb.dsc.jobhub.domain.JobPosting;
import br.ufpb.dsc.jobhub.domain.Seniority;
import br.ufpb.dsc.jobhub.domain.Subscription;
import br.ufpb.dsc.jobhub.domain.UserRole;
import br.ufpb.dsc.jobhub.dto.JobPostForm;
import br.ufpb.dsc.jobhub.service.AuditLogService;
import br.ufpb.dsc.jobhub.service.BillingService;
import br.ufpb.dsc.jobhub.service.JobService;
import br.ufpb.dsc.jobhub.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.validation.BindingResult;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PublicControllerTest {

    private final JobService jobService = mock(JobService.class);
    private final UserService userService = mock(UserService.class);
    private final AuditLogService auditLogService = mock(AuditLogService.class);
    private final BillingService billingService = mock(BillingService.class);
    private final Authentication authentication = mock(Authentication.class);
    private final MockHttpServletRequest request = new MockHttpServletRequest();
    private PublicController controller;
    private AppUser user;

    @BeforeEach
    void setUp() {
        controller = new PublicController(jobService, userService, auditLogService, billingService);
        user = new AppUser("Empresa Pessoa", "empresa@example.com", "empresa", "hash",
                UserRole.ROLE_USER, AuthProvider.LOCAL);
        when(userService.currentUser(authentication)).thenReturn(Optional.of(user));
    }

    @Test
    void publishPageRequiresSubscriptionAndPrefillsAuthenticatedIdentity() {
        RedirectAttributesModelMap redirect = new RedirectAttributesModelMap();
        ExtendedModelMap model = new ExtendedModelMap();

        assertThat(controller.publishJob(model, authentication, redirect))
                .isEqualTo("redirect:/divulgar/assinar");
        assertThat(redirect.getFlashAttributes().get("subscriptionRequired")).isEqualTo(true);

        when(billingService.hasActiveSubscription(user.getEmail())).thenReturn(true);
        assertThat(controller.publishJob(model, authentication, new RedirectAttributesModelMap()))
                .isEqualTo("jobs/post");
        assertThat(model.get("user")).isSameAs(user);
        assertThat(model.get("form")).isInstanceOf(JobPostForm.class);
        JobPostForm form = (JobPostForm) model.get("form");
        assertThat(form.company()).isEqualTo("Empresa Pessoa");
        assertThat(form.companyEmail()).isEqualTo("empresa@example.com");

        Object existing = model.get("form");
        controller.publishJob(model, authentication, new RedirectAttributesModelMap());
        assertThat(model.get("form")).isSameAs(existing);
    }

    @Test
    void createJobRequiresSubscriptionAndPreservesFormValidationErrors() {
        JobPostForm form = validForm();
        BindingResult bindingResult = mock(BindingResult.class);
        RedirectAttributesModelMap redirect = new RedirectAttributesModelMap();
        ExtendedModelMap model = new ExtendedModelMap();

        assertThat(controller.createJob(form, bindingResult, redirect, authentication, request, model))
                .isEqualTo("redirect:/divulgar/assinar");
        verify(jobService, never()).createPending(form);

        when(billingService.hasActiveSubscription(user.getEmail())).thenReturn(true);
        when(bindingResult.hasErrors()).thenReturn(true);
        assertThat(controller.createJob(form, bindingResult, redirect, authentication, request, model))
                .isEqualTo("jobs/post");
        assertThat(model.get("user")).isSameAs(user);
    }

    @Test
    void createsPendingJobWithAuditAndReportsBusinessValidation() {
        JobPostForm form = validForm();
        BindingResult bindingResult = mock(BindingResult.class);
        RedirectAttributesModelMap redirect = new RedirectAttributesModelMap();
        JobPosting job = job();
        when(billingService.hasActiveSubscription(user.getEmail())).thenReturn(true);
        when(jobService.createPending(form)).thenReturn(job);

        assertThat(controller.createJob(form, bindingResult, redirect, authentication, request,
                new ExtendedModelMap())).isEqualTo("redirect:/divulgar");
        assertThat(redirect.getFlashAttributes().get("success")).isEqualTo(true);
        verify(auditLogService).log(request, authentication, "PUBLIC_JOB_SUBMITTED", "JOB_POSTING",
                null, "Vaga enviada pela página pública.");

        when(jobService.createPending(form)).thenThrow(new IllegalArgumentException("Cidade obrigatória"));
        ExtendedModelMap invalidModel = new ExtendedModelMap();
        assertThat(controller.createJob(form, bindingResult, new RedirectAttributesModelMap(), authentication,
                request, invalidModel)).isEqualTo("jobs/post");
        verify(bindingResult).reject("job.location", "Cidade obrigatória");
        assertThat(invalidModel.get("user")).isSameAs(user);
    }

    @Test
    void subscriptionPageAndCheckoutUseAuthenticatedUser() {
        Subscription subscription = new Subscription("Empresa", user.getEmail(), "monthly-basic");
        when(billingService.findByCompanyEmail(user.getEmail())).thenReturn(subscription);
        ExtendedModelMap model = new ExtendedModelMap();

        assertThat(controller.subscribe(model, authentication)).isEqualTo("jobs/subscribe");
        assertThat(model.get("user")).isSameAs(user);
        assertThat(model.get("subscription")).isSameAs(subscription);

        when(billingService.createCheckoutSessionForUser(user))
                .thenReturn(new BillingService.CheckoutResult(subscription, "https://checkout.stripe.test/123"));
        assertThat(controller.subscribeCheckout(authentication, request, new RedirectAttributesModelMap()))
                .isEqualTo("redirect:https://checkout.stripe.test/123");
        verify(auditLogService).log(request, user, "STRIPE_CHECKOUT_STARTED", "SUBSCRIPTION", null,
                "Checkout de assinatura iniciado no Stripe.");
    }

    @Test
    void checkoutFailureAndOutcomePagesProvideClearFeedback() {
        when(billingService.createCheckoutSessionForUser(user))
                .thenThrow(new IllegalStateException("Stripe indisponível"));
        RedirectAttributesModelMap redirect = new RedirectAttributesModelMap();

        assertThat(controller.subscribeCheckout(authentication, request, redirect))
                .isEqualTo("redirect:/divulgar/assinar");
        assertThat(redirect.getFlashAttributes().get("billingError")).isEqualTo("Stripe indisponível");

        ExtendedModelMap success = new ExtendedModelMap();
        ExtendedModelMap canceled = new ExtendedModelMap();
        assertThat(controller.subscriptionSuccess(null, null, request, success)).isEqualTo("admin/billing-success");
        assertThat(success.get("billingMessage").toString()).contains("ativa");
        assertThat(controller.subscriptionCanceled(canceled)).isEqualTo("admin/billing-canceled");
        assertThat(canceled.get("billingMessage").toString()).contains("cancelado");
    }

    @Test
    void subscriptionSuccessActivatesWhenSubscriptionIdAndAuthenticationPresent() {
        when(authentication.isAuthenticated()).thenReturn(true);
        Subscription subscription = new Subscription("Empresa", user.getEmail(), "monthly-basic");
        when(billingService.activateAfterConfirmedPayment(42L, user.getEmail())).thenReturn(subscription);

        ExtendedModelMap model = new ExtendedModelMap();
        assertThat(controller.subscriptionSuccess(42L, authentication, request, model))
                .isEqualTo("admin/billing-success");
        assertThat(model.get("billingMessage").toString()).contains("ativa");
        verify(billingService).activateAfterConfirmedPayment(42L, user.getEmail());
        verify(auditLogService).log(request, user, "STRIPE_SUBSCRIPTION_ACTIVATED",
                "SUBSCRIPTION", 42L, "Assinatura ativada após retorno do checkout do Stripe.");
    }

    @Test
    void subscriptionSuccessSilentlyIgnoresActivationErrors() {
        when(authentication.isAuthenticated()).thenReturn(true);
        when(billingService.activateAfterConfirmedPayment(99L, user.getEmail()))
                .thenThrow(new IllegalArgumentException("Assinatura não encontrada"));

        ExtendedModelMap model = new ExtendedModelMap();
        assertThat(controller.subscriptionSuccess(99L, authentication, request, model))
                .isEqualTo("admin/billing-success");
        assertThat(model.get("billingMessage").toString()).contains("ativa");
    }

    private JobPostForm validForm() {
        return new JobPostForm("Desenvolvedor Java", "Empresa", "empresa@example.com",
                JobLocationType.REMOTE, "", Seniority.JUNIOR, ContractType.CLT, "R$ 5.000",
                "Descrição completa da oportunidade com mais de quarenta caracteres.",
                "Java e Spring", "https://example.com/apply");
    }

    private JobPosting job() {
        return new JobPosting("Desenvolvedor Java", "Empresa", "empresa@example.com",
                JobLocationType.REMOTE, "", Seniority.JUNIOR, ContractType.CLT, "R$ 5.000",
                "Descrição completa da oportunidade.", "Java", "https://example.com/apply");
    }
}
