package br.ufpb.dsc.jobhub.repository;

import br.ufpb.dsc.jobhub.domain.AppUser;
import br.ufpb.dsc.jobhub.domain.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AppUserRepository extends JpaRepository<AppUser, Long> {
    Optional<AppUser> findByEmailIgnoreCase(String email);
    Optional<AppUser> findByUsernameIgnoreCase(String username);
    boolean existsByEmailIgnoreCase(String email);
    boolean existsByUsernameIgnoreCase(String username);
    List<AppUser> findAllByOrderByCreatedAtDesc();
    long countByRole(UserRole role);
}
