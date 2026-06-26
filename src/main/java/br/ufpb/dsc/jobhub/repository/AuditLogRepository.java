package br.ufpb.dsc.jobhub.repository;

import br.ufpb.dsc.jobhub.domain.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    @Query("""
            select a from AuditLog a
            where (:action is null or :action = '' or lower(a.action) like lower(concat('%', :action, '%')))
              and (:actor is null or :actor = '' or lower(coalesce(a.actorEmail, '')) like lower(concat('%', :actor, '%')))
              and (:entityType is null or :entityType = '' or lower(coalesce(a.entityType, '')) like lower(concat('%', :entityType, '%')))
              and (:fromDate is null or a.createdAt >= :fromDate)
              and (:toDate is null or a.createdAt < :toDate)
            order by a.createdAt desc
            """)
    List<AuditLog> search(@Param("action") String action,
                          @Param("actor") String actor,
                          @Param("entityType") String entityType,
                          @Param("fromDate") Instant fromDate,
                          @Param("toDate") Instant toDate);
}
