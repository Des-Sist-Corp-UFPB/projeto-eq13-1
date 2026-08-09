package br.ufpb.dsc.jobhub.service;

import br.ufpb.dsc.jobhub.domain.AppUser;
import br.ufpb.dsc.jobhub.domain.PasswordResetToken;
import br.ufpb.dsc.jobhub.repository.AppUserRepository;
import br.ufpb.dsc.jobhub.repository.PasswordResetTokenRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Locale;
import java.util.Optional;

@Service
public class PasswordResetService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final AppUserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordResetMailService mailService;
    private final String publicUrl;
    private final long ttlMinutes;

    public PasswordResetService(AppUserRepository userRepository,
                                PasswordResetTokenRepository tokenRepository,
                                PasswordEncoder passwordEncoder,
                                PasswordResetMailService mailService,
                                @Value("${app.password-reset.public-url:http://localhost:8080}") String publicUrl,
                                @Value("${app.password-reset.ttl-minutes:30}") long ttlMinutes) {
        this.userRepository = userRepository;
        this.tokenRepository = tokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.mailService = mailService;
        this.publicUrl = publicUrl.replaceAll("/+$", "");
        this.ttlMinutes = ttlMinutes;
    }

    @Transactional
    public void requestReset(String email) {
        String normalizedEmail = email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
        Optional<AppUser> user = userRepository.findByEmailIgnoreCase(normalizedEmail);
        if (user.isEmpty()) {
            hashToken(randomToken());
            return;
        }

        Instant now = Instant.now();
        invalidateActiveTokens(user.get(), now);
        String rawToken = randomToken();
        PasswordResetToken resetToken = new PasswordResetToken(
                user.get(), hashToken(rawToken), now.plus(ttlMinutes, ChronoUnit.MINUTES));
        tokenRepository.save(resetToken);

        try {
            mailService.send(user.get().getEmail(), publicUrl + "/redefinir-senha?token=" + rawToken);
        } catch (RuntimeException exception) {
            resetToken.markUsed(now);
            tokenRepository.save(resetToken);
        }
    }

    @Transactional(readOnly = true)
    public boolean isTokenValid(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            return false;
        }
        return tokenRepository.findByTokenHashAndUsedAtIsNull(hashToken(rawToken))
                .filter(token -> token.isUsable(Instant.now()))
                .isPresent();
    }

    @Transactional
    public Optional<AppUser> resetPassword(String rawToken, String password, String confirmation) {
        if (password == null || password.length() < 8 || password.length() > 72) {
            throw new IllegalArgumentException("A senha deve ter entre 8 e 72 caracteres.");
        }
        if (!password.equals(confirmation)) {
            throw new IllegalArgumentException("As senhas não coincidem.");
        }

        Optional<PasswordResetToken> found = rawToken == null ? Optional.empty()
                : tokenRepository.findByTokenHashAndUsedAtIsNull(hashToken(rawToken));
        if (found.isEmpty()) {
            return Optional.empty();
        }

        PasswordResetToken resetToken = found.get();
        Instant now = Instant.now();
        if (!resetToken.isUsable(now)) {
            resetToken.markUsed(now);
            tokenRepository.save(resetToken);
            return Optional.empty();
        }

        AppUser user = resetToken.getUser();
        user.changePasswordHash(passwordEncoder.encode(password));
        resetToken.markUsed(now);
        invalidateActiveTokens(user, now);
        userRepository.save(user);
        return Optional.of(user);
    }

    private void invalidateActiveTokens(AppUser user, Instant now) {
        tokenRepository.findAllByUserAndUsedAtIsNull(user).forEach(token -> token.markUsed(now));
    }

    private String randomToken() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hashToken(String rawToken) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 indisponivel na JVM", exception);
        }
    }
}
