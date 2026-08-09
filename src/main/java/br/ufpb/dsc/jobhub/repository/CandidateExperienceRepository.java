package br.ufpb.dsc.jobhub.repository;

import br.ufpb.dsc.jobhub.domain.CandidateExperience;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CandidateExperienceRepository extends JpaRepository<CandidateExperience, Long> {
    List<CandidateExperience> findByUserIdOrderByStartedOnDesc(Long userId);
    Optional<CandidateExperience> findByIdAndUserId(Long id, Long userId);
}
