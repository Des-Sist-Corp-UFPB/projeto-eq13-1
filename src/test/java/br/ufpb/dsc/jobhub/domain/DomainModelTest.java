package br.ufpb.dsc.jobhub.domain;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class DomainModelTest {

    @Test
    void appUserMaintainsProfileSecurityAndLifecycleState() {
        AppUser user = new AppUser("Pessoa", "pessoa@example.com", "pessoa", "hash",
                UserRole.ROLE_USER, AuthProvider.LOCAL);

        user.onCreate();
        Instant created = user.getCreatedAt();
        user.updateGoogleProfile("Pessoa Google");
        user.updateGoogleProfile(" ");
        user.changeRole(UserRole.ROLE_ADMIN);
        user.changePasswordHash("novo-hash");
        user.updateProfile("Pessoa Atualizada", "Biografia");
        user.updatePhoto(new byte[]{1, 2}, "image/png");
        user.updatePhotoPosition(30, 70);
        user.updateCover(new byte[]{5, 6}, "image/webp");
        user.updateCoverPosition(45, 65);
        user.updateResume(new byte[]{3, 4}, "curriculo.pdf", "application/pdf");
        user.changeTheme(ThemePreference.DARK);
        user.onUpdate();

        assertThat(user.getId()).isNull();
        assertThat(user.getName()).isEqualTo("Pessoa Atualizada");
        assertThat(user.getEmail()).isEqualTo("pessoa@example.com");
        assertThat(user.getUsername()).isEqualTo("pessoa");
        assertThat(user.getPasswordHash()).isEqualTo("novo-hash");
        assertThat(user.getRole()).isEqualTo(UserRole.ROLE_ADMIN);
        assertThat(user.getProvider()).isEqualTo(AuthProvider.LOCAL);
        assertThat(user.isEnabled()).isTrue();
        assertThat(user.getBiography()).isEqualTo("Biografia");
        assertThat(user.getThemePreference()).isEqualTo(ThemePreference.DARK);
        assertThat(user.getPhotoContent()).containsExactly(1, 2);
        assertThat(user.getPhotoContentType()).isEqualTo("image/png");
        assertThat(user.getPhotoPositionX()).isEqualTo(30);
        assertThat(user.getPhotoPositionY()).isEqualTo(70);
        assertThat(user.getCoverContent()).containsExactly(5, 6);
        assertThat(user.getCoverContentType()).isEqualTo("image/webp");
        assertThat(user.getCoverPositionX()).isEqualTo(45);
        assertThat(user.getCoverPositionY()).isEqualTo(65);
        assertThat(user.getResumeContent()).containsExactly(3, 4);
        assertThat(user.getResumeFileName()).isEqualTo("curriculo.pdf");
        assertThat(user.getResumeContentType()).isEqualTo("application/pdf");
        assertThat(user.getExperiences()).isEmpty();
        assertThat(user.getUpdatedAt()).isAfterOrEqualTo(created);
    }

    @Test
    void experienceExposesOwnedProfessionalHistoryAndLifecycle() {
        AppUser user = user();
        CandidateExperience experience = new CandidateExperience(user, "Dev", "RadarTech",
                LocalDate.of(2024, 1, 1), LocalDate.of(2025, 1, 1), "Plataforma de vagas");

        experience.onCreate();
        Instant created = experience.getCreatedAt();
        experience.onUpdate();

        assertThat(experience.getId()).isNull();
        assertThat(experience.getUser()).isSameAs(user);
        assertThat(experience.getRoleTitle()).isEqualTo("Dev");
        assertThat(experience.getCompany()).isEqualTo("RadarTech");
        assertThat(experience.getStartedOn()).isEqualTo(LocalDate.of(2024, 1, 1));
        assertThat(experience.getEndedOn()).isEqualTo(LocalDate.of(2025, 1, 1));
        assertThat(experience.getDescription()).isEqualTo("Plataforma de vagas");
        assertThat(experience.getUpdatedAt()).isAfterOrEqualTo(created);
    }

    @Test
    void subscriptionRepresentsPendingActiveExpiredAndCanceledStates() {
        Subscription subscription = new Subscription("Empresa", "empresa@example.com", "monthly-basic");
        subscription.onCreate();
        Instant created = subscription.getCreatedAt();

        subscription.markPending("cs_pending");
        assertThat(subscription.isActive()).isFalse();
        subscription.activate(null, "sub_without_expiration");
        assertThat(subscription.isActive()).isTrue();
        subscription.activate(Instant.now().minusSeconds(1), "sub_expired");
        assertThat(subscription.isActive()).isFalse();
        subscription.cancel();
        subscription.onUpdate();

        assertThat(subscription.getId()).isNull();
        assertThat(subscription.getCompany()).isEqualTo("Empresa");
        assertThat(subscription.getCompanyEmail()).isEqualTo("empresa@example.com");
        assertThat(subscription.getPlanCode()).isEqualTo("monthly-basic");
        assertThat(subscription.getStatus()).isEqualTo(SubscriptionStatus.CANCELED);
        assertThat(subscription.getExternalReference()).isEqualTo("sub_expired");
        assertThat(subscription.getValidUntil()).isBefore(Instant.now());
        assertThat(subscription.getUpdatedAt()).isAfterOrEqualTo(created);
    }

    @Test
    void jobPostingTracksModerationViewsAndAllBusinessFields() {
        JobPosting job = job();
        job.onCreate();
        Instant created = job.getCreatedAt();
        job.incrementViews();
        job.publish();
        assertThat(job.getStatus()).isEqualTo(JobStatus.PUBLISHED);
        job.sendToPending();
        assertThat(job.getStatus()).isEqualTo(JobStatus.PENDING);
        job.archive();
        job.onUpdate();

        assertThat(job.getId()).isNull();
        assertThat(job.getTitle()).isEqualTo("Dev Java");
        assertThat(job.getCompany()).isEqualTo("RadarTech");
        assertThat(job.getCompanyEmail()).isEqualTo("vagas@example.com");
        assertThat(job.getLocationType()).isEqualTo(JobLocationType.HYBRID_PB);
        assertThat(job.getCity()).isEqualTo("João Pessoa");
        assertThat(job.getSeniority()).isEqualTo(Seniority.JUNIOR);
        assertThat(job.getContractType()).isEqualTo(ContractType.CLT);
        assertThat(job.getSalaryRange()).isEqualTo("R$ 5.000");
        assertThat(job.getDescription()).isEqualTo("Descrição");
        assertThat(job.getRequirements()).isEqualTo("Spring");
        assertThat(job.getApplyUrl()).isEqualTo("https://example.com");
        assertThat(job.getStatus()).isEqualTo(JobStatus.ARCHIVED);
        assertThat(job.getViews()).isEqualTo(1);
        assertThat(job.getApplications()).isEmpty();
        assertThat(job.getUpdatedAt()).isAfterOrEqualTo(created);
    }

    @Test
    void candidateApplicationSupportsAnonymousAndAuthenticatedCandidates() {
        JobPosting job = job();
        AppUser user = user();
        user.updateResume(new byte[]{7, 8, 9}, "perfil.pdf", "application/pdf");
        CandidateApplication anonymous = new CandidateApplication(
                job, "Visitante", "visitante@example.com", "https://linkedin.com/in/visitante", "Olá");
        CandidateApplication authenticated = new CandidateApplication(
                job, user, "Pessoa", "pessoa@example.com", null, "Tenho interesse");
        authenticated.onCreate();

        assertThat(anonymous.getApplicantUser()).isNull();
        assertThat(anonymous.hasResume()).isFalse();
        assertThat(anonymous.getResumeContent()).isNull();
        assertThat(authenticated.getId()).isNull();
        assertThat(authenticated.getJob()).isSameAs(job);
        assertThat(authenticated.getApplicantUser()).isSameAs(user);
        assertThat(authenticated.getApplicantName()).isEqualTo("Pessoa");
        assertThat(authenticated.getApplicantEmail()).isEqualTo("pessoa@example.com");
        assertThat(authenticated.getLinkedinUrl()).isNull();
        assertThat(authenticated.getMessage()).isEqualTo("Tenho interesse");
        assertThat(authenticated.hasResume()).isTrue();
        assertThat(authenticated.getResumeContent()).containsExactly(7, 8, 9);
        assertThat(authenticated.getResumeFileName()).isEqualTo("perfil.pdf");
        assertThat(authenticated.getResumeContentType()).isEqualTo("application/pdf");
        byte[] detachedResume = authenticated.getResumeContent();
        detachedResume[0] = 0;
        assertThat(authenticated.getResumeContent()).containsExactly(7, 8, 9);
        assertThat(authenticated.getCreatedAt()).isNotNull();
    }

    @Test
    void auditLogPreservesTraceableEventContext() {
        AuditLog log = new AuditLog(7L, "actor@example.com", "JOB_PUBLISHED", "JOB_POSTING", "42",
                "Vaga publicada", "127.0.0.1", "JUnit");
        log.onCreate();

        assertThat(log.getId()).isNull();
        assertThat(log.getActorId()).isEqualTo(7L);
        assertThat(log.getActorEmail()).isEqualTo("actor@example.com");
        assertThat(log.getAction()).isEqualTo("JOB_PUBLISHED");
        assertThat(log.getEntityType()).isEqualTo("JOB_POSTING");
        assertThat(log.getEntityId()).isEqualTo("42");
        assertThat(log.getDescription()).isEqualTo("Vaga publicada");
        assertThat(log.getIpAddress()).isEqualTo("127.0.0.1");
        assertThat(log.getUserAgent()).isEqualTo("JUnit");
        assertThat(log.getCreatedAt()).isNotNull();
    }

    @Test
    void passwordResetTokenTracksExpiryAndSingleUseWithoutStoringRawToken() {
        AppUser user = user();
        Instant expiry = Instant.now().plusSeconds(600);
        PasswordResetToken token = new PasswordResetToken(user, "a".repeat(64), expiry);
        token.onCreate();

        assertThat(token.getId()).isNull();
        assertThat(token.getUser()).isSameAs(user);
        assertThat(token.getTokenHash()).hasSize(64);
        assertThat(token.getExpiresAt()).isEqualTo(expiry);
        assertThat(token.getCreatedAt()).isNotNull();
        assertThat(token.isUsable(Instant.now())).isTrue();
        token.markUsed(Instant.now());
        assertThat(token.getUsedAt()).isNotNull();
        assertThat(token.isUsable(Instant.now())).isFalse();

        PasswordResetToken empty = new PasswordResetToken();
        assertThat(empty.getUser()).isNull();
    }

    private AppUser user() {
        return new AppUser("Pessoa", "pessoa@example.com", "pessoa", "hash",
                UserRole.ROLE_USER, AuthProvider.LOCAL);
    }

    private JobPosting job() {
        return new JobPosting("Dev Java", "RadarTech", "vagas@example.com", JobLocationType.HYBRID_PB,
                "João Pessoa", Seniority.JUNIOR, ContractType.CLT, "R$ 5.000",
                "Descrição", "Spring", "https://example.com");
    }
}
