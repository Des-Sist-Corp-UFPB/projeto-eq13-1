package br.ufpb.dsc.jobhub.config;

import br.ufpb.dsc.jobhub.service.AuditLogService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class AuditAuthenticationFailureHandler implements AuthenticationFailureHandler {

    private final AuditLogService auditLogService;

    public AuditAuthenticationFailureHandler(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception)
            throws IOException, ServletException {
        auditLogService.log(request, request.getParameter("username"), "LOGIN_FAILURE", "AUTH", null, "Falha no login tradicional.");
        response.sendRedirect("/login?error");
    }
}
