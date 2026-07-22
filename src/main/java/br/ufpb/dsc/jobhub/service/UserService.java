package br.ufpb.dsc.jobhub.service;

import br.ufpb.dsc.jobhub.domain.AppUser;
import br.ufpb.dsc.jobhub.domain.AuthProvider;
import br.ufpb.dsc.jobhub.domain.UserRole;
import br.ufpb.dsc.jobhub.dto.GoogleUserProvision;
import br.ufpb.dsc.jobhub.dto.RegistrationForm;
import br.ufpb.dsc.jobhub.repository.AppUserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

@Service
public class UserService implements UserDetailsService {

    private final AppUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(AppUserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String login) throws UsernameNotFoundException {
        AppUser user = findByLogin(login)
                .orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado"));
        return toUserDetails(user);
    }

    @Transactional
    public AppUser registerLocal(RegistrationForm form) {
        String email = normalizeEmail(form.email());
        String username = normalizeUsername(form.username());
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new IllegalArgumentException("Já existe uma conta com este e-mail.");
        }
        if (username != null && userRepository.existsByUsernameIgnoreCase(username)) {
            throw new IllegalArgumentException("Já existe uma conta com este usuário.");
        }
        AppUser user = new AppUser(
                form.name().trim(),
                email,
                username,
                passwordEncoder.encode(form.password()),
                UserRole.ROLE_USER,
                AuthProvider.LOCAL
        );
        return userRepository.save(user);
    }

    @Transactional
    public GoogleUserProvision findOrCreateGoogleUser(String email, String name) {
        String normalizedEmail = normalizeEmail(email);
        Optional<AppUser> existing = userRepository.findByEmailIgnoreCase(normalizedEmail);
        if (existing.isPresent()) {
            AppUser user = existing.get();
            user.updateGoogleProfile(name);
            return new GoogleUserProvision(user, false);
        }
        AppUser user = new AppUser(
                normalizeName(name, normalizedEmail),
                normalizedEmail,
                null,
                passwordEncoder.encode(UUID.randomUUID().toString()),
                UserRole.ROLE_USER,
                AuthProvider.GOOGLE
        );
        return new GoogleUserProvision(userRepository.save(user), true);
    }

    @Transactional
    public AppUser ensureAdminUser(String username, String password) {
        String normalizedUsername = normalizeUsername(username);
        String adminEmail = normalizedUsername + "@radartech.local";
        Optional<AppUser> existing = userRepository.findByUsernameIgnoreCase(normalizedUsername)
                .or(() -> userRepository.findByEmailIgnoreCase(adminEmail));
        if (existing.isPresent()) {
            AppUser user = existing.get();
            user.changeRole(UserRole.ROLE_ADMIN);
            return user;
        }
        AppUser user = new AppUser(
                "Administrador",
                adminEmail,
                normalizedUsername,
                passwordEncoder.encode(password),
                UserRole.ROLE_ADMIN,
                AuthProvider.LOCAL
        );
        return userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public Optional<AppUser> currentUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return Optional.empty();
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof OAuth2User oauth2User) {
            Object email = oauth2User.getAttribute("email");
            if (email != null) {
                return userRepository.findByEmailIgnoreCase(email.toString());
            }
        }
        return findByLogin(authentication.getName());
    }

    @Transactional(readOnly = true)
    public List<AppUser> allUsers() {
        return userRepository.findAllByOrderByCreatedAtDesc();
    }

    @Transactional(readOnly = true)
    public long countUsers() {
        return userRepository.count();
    }

    @Transactional(readOnly = true)
    public Optional<AppUser> findByLogin(String login) {
        String normalized = login == null ? "" : login.trim();
        if (normalized.isBlank()) {
            return Optional.empty();
        }
        return userRepository.findByUsernameIgnoreCase(normalized)
                .or(() -> userRepository.findByEmailIgnoreCase(normalized));
    }

    public UserDetails toUserDetails(AppUser user) {
        return User.builder()
                .username(user.getEmail())
                .password(user.getPasswordHash())
                .disabled(!user.isEnabled())
                .authorities(new SimpleGrantedAuthority(user.getRole().name()))
                .build();
    }

    private String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeUsername(String username) {
        if (username == null || username.isBlank()) {
            return null;
        }
        return username.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeName(String name, String email) {
        if (name != null && !name.isBlank()) {
            return name.trim();
        }
        return email;
    }
}
