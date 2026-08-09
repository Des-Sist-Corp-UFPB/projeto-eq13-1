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
    void rejectsShortPhotoAndOversizedResume() {
        var shortPhoto = new MockMultipartFile("photo", "foto.webp", "image/webp", "RIFF".getBytes());
        assertThatThrownBy(() -> service.update(user, new ProfileUpdateForm("Pessoa", null), shortPhoto, null))
                .isInstanceOf(IllegalArgumentException.class);

        var largeResume = new MockMultipartFile("resume", "curriculo.pdf", "application/pdf",
                new byte[(int) CandidateProfileService.MAX_RESUME_SIZE + 1]);
        assertThatThrownBy(() -> service.update(user, new ProfileUpdateForm("Pessoa", null), null, largeResume))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("5 MB");
    }

    @Test
    void acceptsWebpAndUsesSafeDefaultResumeName() {
        byte[] webp = new byte[]{'R', 'I', 'F', 'F', 0, 0, 0, 0, 'W', 'E', 'B', 'P'};
        var photo = new MockMultipartFile("photo", "foto.webp", "image/webp", webp);
        var resume = new MockMultipartFile("resume", null, "application/pdf", "%PDF-1.7".getBytes());

        var result = service.update(user, new ProfileUpdateForm("Pessoa", null), photo, resume);

        assertThat(result.photoUpdated()).isTrue();
        assertThat(result.resumeUpdated()).isTrue();
        assertThat(user.getPhotoContentType()).isEqualTo("image/webp");
        assertThat(user.getResumeFileName()).isEqualTo("curriculo.pdf");
    }

    @Test
    void acceptsJpegByItsBinarySignature() {
        byte[] jpeg = new byte[]{(byte) 0xFF, (byte) 0xD8, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
        var photo = new MockMultipartFile("photo", "foto.jpg", "image/jpeg", jpeg);

        var result = service.update(user, new ProfileUpdateForm("Pessoa", null), photo, null);

        assertThat(result.photoUpdated()).isTrue();
        assertThat(user.getPhotoContentType()).isEqualTo("image/jpeg");
    }

    @Test
    void updatesPhotoAndCoverWithUserControlledPosition() {
        byte[] png = new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0, 0, 0, 0, 0, 0, 0, 0};
        var photo = new MockMultipartFile("photo", "foto.png", "image/png", png);
        var cover = new MockMultipartFile("cover", "capa.png", "image/png", png);

        assertThat(service.updatePhoto(user, photo, 25, 75)).isTrue();
        assertThat(service.updateCover(user, cover, 40, 60)).isTrue();
        assertThat(service.updatePhoto(user, null, 30, 70)).isFalse();
        assertThat(service.updateCover(user, null, 45, 55)).isFalse();

        assertThat(user.getPhotoPositionX()).isEqualTo(30);
        assertThat(user.getPhotoPositionY()).isEqualTo(70);
        assertThat(user.getCoverContent()).containsExactly(png);
        assertThat(user.getCoverContentType()).isEqualTo("image/png");
        assertThat(user.getCoverPositionX()).isEqualTo(45);
        assertThat(user.getCoverPositionY()).isEqualTo(55);
    }

    @Test
    void rejectsInvalidCoverFilesAndImagePositions() {
        var wrongType = new MockMultipartFile("cover", "capa.gif", "image/gif", new byte[20]);
        assertThatThrownBy(() -> service.updateCover(user, wrongType, 50, 50))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("JPEG, PNG ou WebP");

        var oversized = new MockMultipartFile("cover", "capa.jpg", "image/jpeg",
                new byte[(int) CandidateProfileService.MAX_COVER_SIZE + 1]);
        assertThatThrownBy(() -> service.updateCover(user, oversized, 50, 50))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("5 MB");

        var fakePng = new MockMultipartFile("cover", "capa.png", "image/png", new byte[20]);
        assertThatThrownBy(() -> service.updateCover(user, fakePng, 50, 50))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("não corresponde");

        assertThatThrownBy(() -> service.updatePhoto(user, null, -1, 50))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("entre 0 e 100");
        assertThatThrownBy(() -> service.updateCover(user, null, 50, 101))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("entre 0 e 100");
    }

    @Test
    void wrapsCoverReadFailureAsValidationError() throws IOException {
        MultipartFile cover = mock(MultipartFile.class);
        when(cover.isEmpty()).thenReturn(false);
        when(cover.getContentType()).thenReturn("image/jpeg");
        when(cover.getSize()).thenReturn(100L);
        when(cover.getBytes()).thenThrow(new IOException("falha"));

        assertThatThrownBy(() -> service.updateCover(user, cover, 50, 50))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("capa");
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
