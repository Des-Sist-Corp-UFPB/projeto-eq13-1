package br.ufpb.dsc.jobhub.service;

import br.ufpb.dsc.jobhub.domain.AppUser;
import br.ufpb.dsc.jobhub.domain.CandidateExperience;
import br.ufpb.dsc.jobhub.domain.ThemePreference;
import br.ufpb.dsc.jobhub.dto.ExperienceForm;
import br.ufpb.dsc.jobhub.dto.ProfileUpdateForm;
import br.ufpb.dsc.jobhub.dto.ProfileUpdateResult;
import br.ufpb.dsc.jobhub.repository.AppUserRepository;
import br.ufpb.dsc.jobhub.repository.CandidateExperienceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class CandidateProfileService {

    static final long MAX_PHOTO_SIZE = 3 * 1024 * 1024;
    static final long MAX_RESUME_SIZE = 5 * 1024 * 1024;
    private static final Set<String> PHOTO_TYPES = Set.of("image/jpeg", "image/png", "image/webp");

    private final AppUserRepository userRepository;
    private final CandidateExperienceRepository experienceRepository;

    public CandidateProfileService(AppUserRepository userRepository,
                                   CandidateExperienceRepository experienceRepository) {
        this.userRepository = userRepository;
        this.experienceRepository = experienceRepository;
    }

    @Transactional
    public ProfileUpdateResult update(AppUser user, ProfileUpdateForm form, MultipartFile photo,
                                      MultipartFile resume) {
        user.updateProfile(form.name().trim(), trimToNull(form.biography()));
        boolean photoUpdated = storePhoto(user, photo);
        boolean resumeUpdated = storeResume(user, resume);
        userRepository.save(user);
        return new ProfileUpdateResult(photoUpdated, resumeUpdated);
    }

    @Transactional
    public CandidateExperience addExperience(AppUser user, ExperienceForm form) {
        if (form.endedOn() != null && form.endedOn().isBefore(form.startedOn())) {
            throw new IllegalArgumentException("A data final não pode ser anterior à data inicial.");
        }
        CandidateExperience experience = new CandidateExperience(
                user,
                form.roleTitle().trim(),
                form.company().trim(),
                form.startedOn(),
                form.endedOn(),
                trimToNull(form.description())
        );
        return experienceRepository.save(experience);
    }

    @Transactional
    public CandidateExperience removeExperience(AppUser user, Long experienceId) {
        CandidateExperience experience = experienceRepository.findByIdAndUserId(experienceId, user.getId())
                .orElseThrow(() -> new IllegalArgumentException("Experiência não encontrada."));
        experienceRepository.delete(experience);
        return experience;
    }

    @Transactional
    public AppUser changeTheme(AppUser user, ThemePreference theme) {
        user.changeTheme(theme);
        return userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public List<CandidateExperience> experiences(AppUser user) {
        return experienceRepository.findByUserIdOrderByStartedOnDesc(user.getId());
    }

    private boolean storePhoto(AppUser user, MultipartFile photo) {
        if (photo == null || photo.isEmpty()) {
            return false;
        }
        String contentType = normalizeContentType(photo.getContentType());
        if (!PHOTO_TYPES.contains(contentType)) {
            throw new IllegalArgumentException("A foto deve estar em JPEG, PNG ou WebP.");
        }
        if (photo.getSize() > MAX_PHOTO_SIZE) {
            throw new IllegalArgumentException("A foto deve ter no máximo 3 MB.");
        }
        byte[] content = bytes(photo, "Não foi possível processar a foto.");
        if (!matchesImageSignature(content, contentType)) {
            throw new IllegalArgumentException("O conteúdo da foto não corresponde ao formato informado.");
        }
        user.updatePhoto(content, contentType);
        return true;
    }

    private boolean storeResume(AppUser user, MultipartFile resume) {
        if (resume == null || resume.isEmpty()) {
            return false;
        }
        if (!"application/pdf".equals(normalizeContentType(resume.getContentType()))) {
            throw new IllegalArgumentException("O currículo deve estar em formato PDF.");
        }
        if (resume.getSize() > MAX_RESUME_SIZE) {
            throw new IllegalArgumentException("O currículo deve ter no máximo 5 MB.");
        }
        byte[] content = bytes(resume, "Não foi possível processar o currículo.");
        if (content.length < 5 || !new String(content, 0, 5, StandardCharsets.US_ASCII).equals("%PDF-")) {
            throw new IllegalArgumentException("O arquivo enviado não é um PDF válido.");
        }
        user.updateResume(content, safeFileName(resume.getOriginalFilename()), "application/pdf");
        return true;
    }

    private byte[] bytes(MultipartFile file, String message) {
        try {
            return file.getBytes();
        } catch (IOException ex) {
            throw new IllegalArgumentException(message, ex);
        }
    }

    private boolean matchesImageSignature(byte[] content, String contentType) {
        if (content.length < 12) {
            return false;
        }
        return switch (contentType) {
            case "image/jpeg" -> content[0] == (byte) 0xFF && content[1] == (byte) 0xD8;
            case "image/png" -> content[0] == (byte) 0x89 && content[1] == 0x50
                    && content[2] == 0x4E && content[3] == 0x47;
            case "image/webp" -> ascii(content, 0, 4).equals("RIFF") && ascii(content, 8, 4).equals("WEBP");
            default -> false;
        };
    }

    private String ascii(byte[] content, int offset, int length) {
        return new String(content, offset, length, StandardCharsets.US_ASCII);
    }

    private String safeFileName(String originalFileName) {
        if (originalFileName == null || originalFileName.isBlank()) {
            return "curriculo.pdf";
        }
        String normalized = originalFileName.replace('\\', '/');
        String fileName = normalized.substring(normalized.lastIndexOf('/') + 1).trim();
        return fileName.toLowerCase(Locale.ROOT).endsWith(".pdf") ? fileName : fileName + ".pdf";
    }

    private String normalizeContentType(String contentType) {
        return contentType == null ? "" : contentType.toLowerCase(Locale.ROOT).trim();
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
