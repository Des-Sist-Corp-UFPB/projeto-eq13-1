package br.ufpb.dsc.jobhub.config;

import br.ufpb.dsc.jobhub.service.AuditLogService;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class AuthenticationHandlersTest {

    private final AuditLogService auditLogService = mock(AuditLogService.class);
    private final MockHttpServletRequest request = new MockHttpServletRequest();

    @Test
    void traditionalSuccessAuditsAndRoutesEachRole() throws Exception {
        var handler = new AuditAuthenticationSuccessHandler(auditLogService);

        assertThat(successRedirect(handler, auth("admin", "ROLE_ADMIN"))).isEqualTo("/admin");
        assertThat(successRedirect(handler, auth("pessoa", "ROLE_USER"))).isEqualTo("/minha-conta");
        verify(auditLogService, org.mockito.Mockito.times(2)).log(
                org.mockito.ArgumentMatchers.eq(request),
                org.mockito.ArgumentMatchers.any(Authentication.class),
                org.mockito.ArgumentMatchers.eq("LOGIN_SUCCESS"),
                org.mockito.ArgumentMatchers.eq("AUTH"),
                org.mockito.ArgumentMatchers.isNull(),
                org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void googleSuccessAuditsAndRoutesEachRole() throws Exception {
        var handler = new AuditOAuth2SuccessHandler(auditLogService);

        assertThat(successRedirect(handler, auth("admin", "ROLE_ADMIN"))).isEqualTo("/admin");
        assertThat(successRedirect(handler, auth("pessoa", "ROLE_USER"))).isEqualTo("/minha-conta");
        verify(auditLogService, org.mockito.Mockito.times(2)).log(
                org.mockito.ArgumentMatchers.eq(request),
                org.mockito.ArgumentMatchers.any(Authentication.class),
                org.mockito.ArgumentMatchers.eq("GOOGLE_LOGIN_SUCCESS"),
                org.mockito.ArgumentMatchers.eq("AUTH"),
                org.mockito.ArgumentMatchers.isNull(),
                org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void failedLoginAuditsSubmittedIdentityAndReturnsToLogin() throws Exception {
        request.setParameter("username", "falha@example.com");
        MockHttpServletResponse response = new MockHttpServletResponse();

        new AuditAuthenticationFailureHandler(auditLogService)
                .onAuthenticationFailure(request, response, new BadCredentialsException("inválida"));

        assertThat(response.getRedirectedUrl()).isEqualTo("/login?error");
        verify(auditLogService).log(request, "falha@example.com", "LOGIN_FAILURE", "AUTH", null,
                "Falha no login tradicional.");
    }

    @Test
    void logoutAuditsAuthenticatedSessionAndStillHandlesExpiredSession() throws Exception {
        var handler = new AuditLogoutSuccessHandler(auditLogService);
        Authentication authentication = auth("pessoa", "ROLE_USER");
        MockHttpServletResponse authenticatedResponse = new MockHttpServletResponse();

        handler.onLogoutSuccess(request, authenticatedResponse, authentication);

        assertThat(authenticatedResponse.getRedirectedUrl()).isEqualTo("/?logout");
        verify(auditLogService).log(request, authentication, "LOGOUT", "AUTH", null, "Sessão encerrada.");

        AuditLogService noSessionAudit = mock(AuditLogService.class);
        MockHttpServletResponse expiredResponse = new MockHttpServletResponse();
        new AuditLogoutSuccessHandler(noSessionAudit).onLogoutSuccess(request, expiredResponse, null);
        assertThat(expiredResponse.getRedirectedUrl()).isEqualTo("/?logout");
        verify(noSessionAudit, never()).log(
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(Authentication.class),
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    private String successRedirect(org.springframework.security.web.authentication.AuthenticationSuccessHandler handler,
                                   Authentication authentication) throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        handler.onAuthenticationSuccess(request, response, authentication);
        return response.getRedirectedUrl();
    }

    private Authentication auth(String username, String role) {
        return new UsernamePasswordAuthenticationToken(
                username, "ignored", List.of(new SimpleGrantedAuthority(role)));
    }
}
