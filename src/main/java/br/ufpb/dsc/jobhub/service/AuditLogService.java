package br.ufpb.dsc.jobhub.service;

import br.ufpb.dsc.jobhub.domain.AppUser;
import br.ufpb.dsc.jobhub.domain.AuditLog;
import br.ufpb.dsc.jobhub.repository.AuditLogRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

@Service
public class AuditLogService {

    private static final ZoneId DEFAULT_ZONE = ZoneId.of("America/Sao_Paulo");

    private final AuditLogRepository auditLogRepository;
    private final UserService userService;

    public AuditLogService(AuditLogRepository auditLogRepository, UserService userService) {
        this.auditLogRepository = auditLogRepository;
        this.userService = userService;
    }

    @Transactional
    public AuditLog log(HttpServletRequest request, Authentication authentication, String action,
                        String entityType, Object entityId, String description) {
        Optional<AppUser> currentUser = userService.currentUser(authentication);
        return log(request,
                currentUser.map(AppUser::getId).orElse(null),
                currentUser.map(AppUser::getEmail).orElse(null),
                action,
                entityType,
                entityId,
                description);
    }

    @Transactional
    public AuditLog log(HttpServletRequest request, AppUser actor, String action,
                        String entityType, Object entityId, String description) {
        return log(request,
                actor == null ? null : actor.getId(),
                actor == null ? null : actor.getEmail(),
                action,
                entityType,
                entityId,
                description);
    }

    @Transactional
    public AuditLog log(HttpServletRequest request, String actorEmail, String action,
                        String entityType, Object entityId, String description) {
        return log(request, null, actorEmail, action, entityType, entityId, description);
    }

    @Transactional
    public AuditLog logSystem(String actorEmail, String action, String entityType, Object entityId, String description) {
        AuditLog auditLog = new AuditLog(null, actorEmail, action, entityType, stringify(entityId), description, null, null);
        return auditLogRepository.save(auditLog);
    }

    @Transactional(readOnly = true)
    public List<AuditLog> search(String action, String actor, String entityType, LocalDate from, LocalDate to) {
        Instant fromInstant = from == null ? null : from.atStartOfDay(DEFAULT_ZONE).toInstant();
        Instant toInstant = to == null ? null : to.plusDays(1).atStartOfDay(DEFAULT_ZONE).toInstant();
        return auditLogRepository.search(action, actor, entityType, fromInstant, toInstant, PageRequest.of(0, 200));
    }

    private AuditLog log(HttpServletRequest request, Long actorId, String actorEmail, String action,
                         String entityType, Object entityId, String description) {
        AuditLog auditLog = new AuditLog(actorId, actorEmail, action, entityType, stringify(entityId),
                description, ipAddress(request), userAgent(request));
        return auditLogRepository.save(auditLog);
    }

    private String stringify(Object entityId) {
        return entityId == null ? null : entityId.toString();
    }

    private String ipAddress(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private String userAgent(HttpServletRequest request) {
        return request == null ? null : request.getHeader("User-Agent");
    }
}
