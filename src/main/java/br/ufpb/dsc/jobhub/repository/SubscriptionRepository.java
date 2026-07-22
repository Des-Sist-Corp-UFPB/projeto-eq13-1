package br.ufpb.dsc.jobhub.repository;

import br.ufpb.dsc.jobhub.domain.Subscription;
import br.ufpb.dsc.jobhub.domain.SubscriptionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {
    Optional<Subscription> findByCompanyEmailIgnoreCase(String companyEmail);

    long countByStatus(SubscriptionStatus status);
}