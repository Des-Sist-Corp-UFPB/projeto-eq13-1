package br.ufpb.dsc.jobhub.config;

import br.ufpb.dsc.jobhub.service.AuditLogService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class AuditOAuth2SuccessHandler implements AuthenticationSuccessHandler {

    private final AuditLogService auditLogService;

    public AuditOAuth2SuccessHandler(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication)
            throws IOException, ServletException {
        auditLogService.log(request, authentication, "GOOGLE_LOGIN_SUCCESS", "AUTH", null, "Login via Google OAuth2 realizado com sucesso.");
        response.sendRedirect(hasRole(authentication, "ROLE_ADMIN") ? "/admin" : "/minha-conta");
    }

    private boolean hasRole(Authentication authentication, String role) {
        return authentication.getAuthorities().stream().anyMatch(authority -> authority.getAuthority().equals(role));
    }
}
