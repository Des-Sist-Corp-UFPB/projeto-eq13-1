package br.ufpb.dsc.jobhub.config;

import br.ufpb.dsc.jobhub.service.AuditLogService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class AuditLogoutSuccessHandler implements LogoutSuccessHandler {

    private final AuditLogService auditLogService;

    public AuditLogoutSuccessHandler(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    @Override
    public void onLogoutSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication)
            throws IOException, ServletException {
        if (authentication != null) {
            auditLogService.log(request, authentication, "LOGOUT", "AUTH", null, "Sessão encerrada.");
        }
        response.sendRedirect("/?logout");
    }
}
