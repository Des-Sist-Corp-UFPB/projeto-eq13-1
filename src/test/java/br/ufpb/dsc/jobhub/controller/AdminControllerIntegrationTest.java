package br.ufpb.dsc.jobhub.controller;

import br.ufpb.dsc.jobhub.domain.Subscription;
import br.ufpb.dsc.jobhub.domain.SubscriptionStatus;
import br.ufpb.dsc.jobhub.service.BillingService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Fluxos administrativos e cobrança")
class AdminControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BillingService billingService;

    @Test
    void billingCheckoutSuccessFlow() throws Exception {
        Subscription sub = new Subscription("Empresa Teste", "teste@empresa.com", "monthly-basic");
        sub.activate(Instant.now().plusSeconds(3600), "cs_123");
        BillingService.CheckoutResult result = new BillingService.CheckoutResult(sub, "https://checkout.stripe.com/pay/cs_123");

        when(billingService.createCheckoutSession("Empresa Teste", "teste@empresa.com"))
                .thenReturn(result);

        mockMvc.perform(post("/admin/billing/checkout")
                        .with(csrf())
                        .with(user("admin@radartech.local").roles("ADMIN"))
                        .param("company", "Empresa Teste")
                        .param("companyEmail", "teste@empresa.com"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("https://checkout.stripe.com/pay/cs_123"));
    }

    @Test
    void billingCheckoutErrorFlow() throws Exception {
        when(billingService.createCheckoutSession("Empresa Teste", "teste@empresa.com"))
                .thenThrow(new IllegalStateException("Configuração do Stripe ausente"));

        mockMvc.perform(post("/admin/billing/checkout")
                        .with(csrf())
                        .with(user("admin@radartech.local").roles("ADMIN"))
                        .param("company", "Empresa Teste")
                        .param("companyEmail", "teste@empresa.com"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin"))
                .andExpect(flash().attribute("billingMessage", "Configuração do Stripe ausente"));
    }

    @Test
    void billingSuccessAndCanceledPages() throws Exception {
        mockMvc.perform(get("/admin/billing/sucesso")
                        .with(user("admin@radartech.local").roles("ADMIN"))
                        .param("subscription", "123"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/billing-success"))
                .andExpect(model().attribute("billingMessage", "Assinatura iniciada com sucesso."));

        mockMvc.perform(get("/admin/billing/sucesso")
                        .with(user("admin@radartech.local").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/billing-success"))
                .andExpect(model().attribute("billingMessage", "Assinatura iniciada."));

        mockMvc.perform(get("/admin/billing/cancelado")
                        .with(user("admin@radartech.local").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/billing-canceled"))
                .andExpect(model().attribute("billingMessage", "Checkout cancelado."));
    }

    @Test
    void adminDashboardNullAuth() throws Exception {
        mockMvc.perform(get("/admin"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    void adminDashboardCustomUserNamesAndEmails() throws Exception {
        // Test normal user details from login (contains @)
        mockMvc.perform(get("/admin")
                        .with(user("custom-admin@test.com").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/dashboard"))
                .andExpect(model().attribute("billingCompany", "custom-admin@test.com"))
                .andExpect(model().attribute("billingCompanyEmail", "custom-admin@test.com"));

        // Test user details from login (does NOT contain @)
        mockMvc.perform(get("/admin")
                        .with(user("adminlocal").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/dashboard"))
                .andExpect(model().attribute("billingCompany", "adminlocal"))
                .andExpect(model().attribute("billingCompanyEmail", "adminlocal@radartech.local"));

        // Test empty/blank username
        mockMvc.perform(get("/admin")
                        .with(user(" ").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/dashboard"))
                .andExpect(model().attribute("billingCompany", "RadarTech PB"))
                .andExpect(model().attribute("billingCompanyEmail", "admin@radartech.local"));
    }
}
