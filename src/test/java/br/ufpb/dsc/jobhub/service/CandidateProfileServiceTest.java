package br.ufpb.dsc.jobhub.service;

import br.ufpb.dsc.jobhub.domain.AppUser;
import br.ufpb.dsc.jobhub.domain.AuthProvider;
import br.ufpb.dsc.jobhub.domain.CandidateExperience;
import br.ufpb.dsc.jobhub.domain.ThemePreference;
import br.ufpb.dsc.jobhub.domain.UserRole;
import br.ufpb.dsc.jobhub.dto.ExperienceForm;
import br.ufpb.dsc.jobhub.dto.ProfileUpdateForm;
import br.ufpb.dsc.jobhub.repository.AppUserRepository;
import br.ufpb.dsc.jobhub.repository.CandidateExperienceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CandidateProfileServiceTest {

    private AppUserRepository userRepository;
    private CandidateExperienceRepository experienceRepository;
    private CandidateProfileService service;
    private AppUser user;

    @BeforeEach
    void setUp() {
        userRepository = mock(AppUserRepository.class);
        experienceRepository = mock(CandidateExperienceRepository.class);
        service = new CandidateProfileService(userRepository, experienceRepository);
        user = new AppUser("Pessoa", "pessoa@example.com", "pessoa", "hash", UserRole.ROLE_USER, AuthProvider.LOCAL);
    }

    @Test
    void updatesProfilePhotoAndResumeWithValidatedContent() {
        byte[] png = new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0, 0, 0, 0, 0, 0, 0, 0};
        var photo = new MockMultipartFile("photo", "foto.png", "image/png", png);
        var resume = new MockMultipartFile("resume", "../curriculo", "application/pdf", "%PDF-1.7".getBytes());

        var result = service.update(user, new ProfileUpdateForm(" Pessoa Atualizada ", " Bio profissional "), photo, resume);

        assertThat(result.photoUpdated()).isTrue();
        assertThat(result.resumeUpdated()).isTrue();
        assertThat(user.getName()).isEqualTo("Pessoa Atualizada");
        assertThat(user.getBiography()).isEqualTo("Bio profissional");
        assertThat(user.getPhotoContentType()).isEqualTo("image/png");
        assertThat(user.getResumeFileName()).isEqualTo("curriculo.pdf");
        verify(userRepository).save(user);
    }

    @Test
    void updateWithoutFilesClearsBlankBiographyAndKeepsAssetsUnchanged() {
        var result = service.update(user, new ProfileUpdateForm("Pessoa", " "), null, null);

        assertThat(result.photoUpdated()).isFalse();
        assertThat(result.resumeUpdated()).isFalse();
        assertThat(user.getBiography()).isNull();
    }

    @Test
    void rejectsInvalidOrOversizedProfileFiles() {
        var wrongPhoto = new MockMultipartFile("photo", "foto.gif", "image/gif", new byte[20]);
        assertThatThrownBy(() -> service.update(user, new ProfileUpdateForm("Pessoa", null), wrongPhoto, null))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("JPEG, PNG ou WebP");

        var fakePng = new MockMultipartFile("photo", "foto.png", "image/png", new byte[20]);
        assertThatThrownBy(() -> service.update(user, new ProfileUpdateForm("Pessoa", null), fakePng, null))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("não corresponde");

        var largePhoto = new MockMultipartFile("photo", "foto.jpg", "image/jpeg",
                new byte[(int) CandidateProfileService.MAX_PHOTO_SIZE + 1]);
        assertThatThrownBy(() -> service.update(user, new ProfileUpdateForm("Pessoa", null), largePhoto, null))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("3 MB");

        var wrongResume = new MockMultipartFile("resume", "curriculo.txt", "text/plain", "texto".getBytes());
        assertThatThrownBy(() -> service.update(user, new ProfileUpdateForm("Pessoa", null), null, wrongResume))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("PDF");

        var fakeResume = new MockMultipartFile("resume", "curriculo.pdf", "application/pdf", "arquivo".getBytes());
        assertThatThrownBy(() -> service.update(user, new ProfileUpdateForm("Pessoa", null), null, fakeResume))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("PDF válido");
    }

    @Test
    void wrapsFileReadFailuresAsValidationErrors() throws IOException {
        MultipartFile photo = mock(MultipartFile.class);
        when(photo.isEmpty()).thenReturn(false);
        when(photo.getContentType()).thenReturn("image/jpeg");
        when(photo.getSize()).thenReturn(100L);
        when(photo.getBytes()).thenThrow(new IOException("falha"));

        assertThatThrownBy(() -> service.update(user, new ProfileUpdateForm("Pessoa", null), photo, null))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("foto");
    }

    @Test
    void managesExperiencesAndThemeOnlyForTheirOwner() {
        ExperienceForm form = new ExperienceForm("Dev", "Radar", LocalDate.of(2024, 1, 1), null, "APIs");
        CandidateExperience saved = new CandidateExperience(user, "Dev", "Radar", form.startedOn(), null, "APIs");
        when(experienceRepository.save(org.mockito.ArgumentMatchers.any())).thenReturn(saved);
        when(experienceRepository.findByUserIdOrderByStartedOnDesc(user.getId())).thenReturn(List.of(saved));
        when(experienceRepository.findByIdAndUserId(10L, user.getId())).thenReturn(Optional.of(saved));

        assertThat(service.addExperience(user, form).getCompany()).isEqualTo("Radar");
        assertThat(service.experiences(user)).containsExactly(saved);
        assertThat(service.removeExperience(user, 10L)).isSameAs(saved);
        verify(experienceRepository).delete(saved);

        service.changeTheme(user, ThemePreference.DARK);
        assertThat(user.getThemePreference()).isEqualTo(ThemePreference.DARK);
        verify(userRepository).save(user);
    }

    @Test
    void rejectsInvertedExperiencePeriodAndMissingExperience() {
        ExperienceForm inverted = new ExperienceForm("Dev", "Radar", LocalDate.of(2025, 1, 1),
                LocalDate.of(2024, 1, 1), null);
        assertThatThrownBy(() -> service.addExperience(user, inverted))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("data final");
        when(experienceRepository.findByIdAndUserId(99L, user.getId())).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.removeExperience(user, 99L))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("não encontrada");
    }
}
