package br.ufpb.dsc.jobhub.repository;

import br.ufpb.dsc.jobhub.domain.CandidateApplication;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;

public interface CandidateApplicationRepository extends JpaRepository<CandidateApplication, Long> {
    List<CandidateApplication> findAllByOrderByCreatedAtDesc();
    long countByCreatedAtAfter(Instant createdAt);
}
