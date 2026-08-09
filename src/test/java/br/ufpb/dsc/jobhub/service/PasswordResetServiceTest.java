package br.ufpb.dsc.jobhub.service;

import br.ufpb.dsc.jobhub.domain.AppUser;
import br.ufpb.dsc.jobhub.domain.AuthProvider;
import br.ufpb.dsc.jobhub.domain.PasswordResetToken;
import br.ufpb.dsc.jobhub.domain.UserRole;
import br.ufpb.dsc.jobhub.repository.AppUserRepository;
import br.ufpb.dsc.jobhub.repository.PasswordResetTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PasswordResetServiceTest {

    private AppUserRepository userRepository;
    private PasswordResetTokenRepository tokenRepository;
    private PasswordEncoder passwordEncoder;
    private PasswordResetMailService mailService;
    private PasswordResetService service;
    private AppUser user;

    @BeforeEach
    void setUp() {
        userRepository = mock(AppUserRepository.class);
        tokenRepository = mock(PasswordResetTokenRepository.class);
        passwordEncoder = mock(PasswordEncoder.class);
        mailService = mock(PasswordResetMailService.class);
        service = new PasswordResetService(userRepository, tokenRepository, passwordEncoder, mailService,
                "https://eq13.dsc.rodrigor.com/", 30);
        user = new AppUser("Pessoa", "pessoa@example.com", "pessoa", "hash",
                UserRole.ROLE_USER, AuthProvider.LOCAL);
    }

    @Test
    void unknownEmailReceivesTheSameSilentTreatment() {
        when(userRepository.findByEmailIgnoreCase("desconhecido@example.com")).thenReturn(Optional.empty());

        service.requestReset(" DESCONHECIDO@example.com ");

        verify(tokenRepository, never()).save(any());
        verify(mailService, never()).send(any(), any());
    }

    @Test
    void handlesNullEmailAndReportsUnavailableSha256() {
        when(userRepository.findByEmailIgnoreCase("")).thenReturn(Optional.empty());

        try (MockedStatic<MessageDigest> digest = mockStatic(MessageDigest.class)) {
            digest.when(() -> MessageDigest.getInstance("SHA-256"))
                    .thenThrow(new NoSuchAlgorithmException("indisponivel"));

            assertThatThrownBy(() -> service.requestReset(null))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("SHA-256");
        }
    }

    @Test
    void createsHashedSingleUseTokenInvalidatesPreviousAndSendsLink() {
        PasswordResetToken previous = token(Instant.now().plusSeconds(600));
        when(userRepository.findByEmailIgnoreCase("pessoa@example.com")).thenReturn(Optional.of(user));
        when(tokenRepository.findAllByUserAndUsedAtIsNull(user)).thenReturn(List.of(previous));

        service.requestReset("PESSOA@example.com");

        ArgumentCaptor<PasswordResetToken> saved = ArgumentCaptor.forClass(PasswordResetToken.class);
        verify(tokenRepository).save(saved.capture());
        assertThat(previous.getUsedAt()).isNotNull();
        assertThat(saved.getValue().getTokenHash()).hasSize(64).doesNotContain("=");
        assertThat(saved.getValue().getExpiresAt()).isAfter(Instant.now().plusSeconds(1_700));
        verify(mailService).send(eq("pessoa@example.com"),
                contains("https://eq13.dsc.rodrigor.com/redefinir-senha?token="));
    }

    @Test
    void invalidatesTokenWhenMailDeliveryFails() {
        when(userRepository.findByEmailIgnoreCase("pessoa@example.com")).thenReturn(Optional.of(user));
        when(tokenRepository.findAllByUserAndUsedAtIsNull(user)).thenReturn(List.of());
        doThrow(new IllegalStateException("SMTP indisponível")).when(mailService).send(any(), any());

        service.requestReset("pessoa@example.com");

        ArgumentCaptor<PasswordResetToken> saved = ArgumentCaptor.forClass(PasswordResetToken.class);
        verify(tokenRepository, org.mockito.Mockito.times(2)).save(saved.capture());
        assertThat(saved.getAllValues().getLast().getUsedAt()).isNotNull();
    }

    @Test
    void validatesOnlyPresentUnexpiredTokens() {
        PasswordResetToken valid = token(Instant.now().plusSeconds(600));
        PasswordResetToken expired = token(Instant.now().minusSeconds(1));

        assertThat(service.isTokenValid(null)).isFalse();
        assertThat(service.isTokenValid(" ")).isFalse();
        when(tokenRepository.findByTokenHashAndUsedAtIsNull(any())).thenReturn(Optional.empty());
        assertThat(service.isTokenValid("missing")).isFalse();
        when(tokenRepository.findByTokenHashAndUsedAtIsNull(any())).thenReturn(Optional.of(valid));
        assertThat(service.isTokenValid("valid")).isTrue();
        when(tokenRepository.findByTokenHashAndUsedAtIsNull(any())).thenReturn(Optional.of(expired));
        assertThat(service.isTokenValid("expired")).isFalse();
    }

    @Test
    void rejectsUnsafeOrMismatchedPasswords() {
        assertThatThrownBy(() -> service.resetPassword("token", null, null))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("8 e 72");
        assertThatThrownBy(() -> service.resetPassword("token", "curta", "curta"))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("8 e 72");
        assertThatThrownBy(() -> service.resetPassword("token", "x".repeat(73), "x".repeat(73)))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("8 e 72");
        assertThatThrownBy(() -> service.resetPassword("token", "senha-segura", "outra-senha"))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("não coincidem");
    }

    @Test
    void rejectsMissingAndExpiredResetTokens() {
        assertThat(service.resetPassword(null, "senha-segura", "senha-segura")).isEmpty();
        when(tokenRepository.findByTokenHashAndUsedAtIsNull(any())).thenReturn(Optional.empty());
        assertThat(service.resetPassword("missing", "senha-segura", "senha-segura")).isEmpty();

        PasswordResetToken expired = token(Instant.now().minusSeconds(1));
        when(tokenRepository.findByTokenHashAndUsedAtIsNull(any())).thenReturn(Optional.of(expired));
        assertThat(service.resetPassword("expired", "senha-segura", "senha-segura")).isEmpty();
        assertThat(expired.getUsedAt()).isNotNull();
        verify(tokenRepository).save(expired);
    }

    @Test
    void updatesPasswordWithBcryptCompatibleEncoderAndConsumesAllTokens() {
        PasswordResetToken current = token(Instant.now().plusSeconds(600));
        PasswordResetToken sibling = token(Instant.now().plusSeconds(700));
        when(tokenRepository.findByTokenHashAndUsedAtIsNull(any())).thenReturn(Optional.of(current));
        when(tokenRepository.findAllByUserAndUsedAtIsNull(user)).thenReturn(List.of(current, sibling));
        when(passwordEncoder.encode("senha-segura")).thenReturn("bcrypt-hash");

        Optional<AppUser> updated = service.resetPassword("valid", "senha-segura", "senha-segura");

        assertThat(updated).contains(user);
        assertThat(user.getPasswordHash()).isEqualTo("bcrypt-hash");
        assertThat(current.getUsedAt()).isNotNull();
        assertThat(sibling.getUsedAt()).isNotNull();
        verify(userRepository).save(user);
    }

    private PasswordResetToken token(Instant expiry) {
        return new PasswordResetToken(user, "a".repeat(64), expiry);
    }
}
