package br.ufpb.dsc.jobhub.controller;

import br.ufpb.dsc.jobhub.domain.AppUser;
import br.ufpb.dsc.jobhub.domain.ThemePreference;
import br.ufpb.dsc.jobhub.dto.ExperienceForm;
import br.ufpb.dsc.jobhub.dto.ProfileUpdateForm;
import br.ufpb.dsc.jobhub.dto.ProfileUpdateResult;
import br.ufpb.dsc.jobhub.dto.RegistrationForm;
import br.ufpb.dsc.jobhub.service.AuditLogService;
import br.ufpb.dsc.jobhub.service.CandidateProfileService;
import br.ufpb.dsc.jobhub.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Locale;

@Controller
public class AuthController {

    private final UserService userService;
    private final CandidateProfileService profileService;
    private final AuditLogService auditLogService;

    public AuthController(UserService userService, CandidateProfileService profileService,
                          AuditLogService auditLogService) {
        this.userService = userService;
        this.profileService = profileService;
        this.auditLogService = auditLogService;
    }

    @GetMapping("/login")
    public String login(Authentication authentication) {
        if (isAuthenticated(authentication)) {
            boolean admin = authentication.getAuthorities().stream()
                    .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));
            return admin ? "redirect:/admin" : "redirect:/minha-conta";
        }
        return "auth/login";
    }

    @GetMapping("/cadastro")
    public String register(Model model) {
        if (!model.containsAttribute("form")) {
            model.addAttribute("form", RegistrationForm.empty());
        }
        return "auth/register";
    }

    @PostMapping("/cadastro")
    public String register(@Valid @ModelAttribute("form") RegistrationForm form,
                           BindingResult bindingResult,
                           RedirectAttributes redirectAttributes,
                           HttpServletRequest request) {
        if (bindingResult.hasErrors()) {
            return "auth/register";
        }
        try {
            AppUser user = userService.registerLocal(form);
            auditLogService.log(request, user, "USER_REGISTER", "APP_USER", user.getId(), "Cadastro tradicional de usuário.");
            redirectAttributes.addFlashAttribute("registered", true);
            return "redirect:/login";
        } catch (IllegalArgumentException ex) {
            bindingResult.reject("registration.error", ex.getMessage());
            return "auth/register";
        }
    }

    @GetMapping("/minha-conta")
    public String profile(Authentication authentication, Model model) {
        return profileView(currentUser(authentication), model);
    }

    @PostMapping(value = "/minha-conta", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public String updateProfile(@Valid @ModelAttribute("profileForm") ProfileUpdateForm form,
                                BindingResult bindingResult,
                                @RequestParam(required = false) MultipartFile photo,
                                @RequestParam(required = false) MultipartFile resume,
                                Authentication authentication,
                                HttpServletRequest request,
                                Model model,
                                RedirectAttributes redirectAttributes) {
        AppUser user = currentUser(authentication);
        if (bindingResult.hasErrors()) {
            return profileView(user, model);
        }
        try {
            ProfileUpdateResult result = profileService.update(user, form, photo, resume);
            auditLogService.log(request, user, "PROFILE_UPDATED", "APP_USER", user.getId(),
                    "Perfil do candidato atualizado.");
            if (result.photoUpdated()) {
                auditLogService.log(request, user, "PROFILE_PHOTO_UPDATED", "APP_USER", user.getId(),
                        "Foto do perfil atualizada.");
            }
            if (result.resumeUpdated()) {
                auditLogService.log(request, user, "PROFILE_RESUME_UPDATED", "APP_USER", user.getId(),
                        "Currículo PDF atualizado.");
            }
            redirectAttributes.addFlashAttribute("profileSaved", true);
            return "redirect:/minha-conta";
        } catch (IllegalArgumentException ex) {
            bindingResult.reject("profile.file", ex.getMessage());
            return profileView(user, model);
        }
    }

    @PostMapping("/minha-conta/experiencias")
    public String addExperience(@Valid @ModelAttribute("experienceForm") ExperienceForm form,
                                BindingResult bindingResult,
                                Authentication authentication,
                                HttpServletRequest request,
                                Model model,
                                RedirectAttributes redirectAttributes) {
        AppUser user = currentUser(authentication);
        if (bindingResult.hasErrors()) {
            model.addAttribute("profileForm", new ProfileUpdateForm(user.getName(), user.getBiography()));
            model.addAttribute("experienceHasErrors", true);
            return profileView(user, model);
        }
        try {
            var experience = profileService.addExperience(user, form);
            auditLogService.log(request, user, "PROFILE_EXPERIENCE_ADDED", "CANDIDATE_EXPERIENCE",
                    experience.getId(), "Experiência profissional adicionada ao perfil.");
            redirectAttributes.addFlashAttribute("experienceSaved", true);
            return "redirect:/minha-conta#experiencias";
        } catch (IllegalArgumentException ex) {
            bindingResult.reject("experience.period", ex.getMessage());
            model.addAttribute("profileForm", new ProfileUpdateForm(user.getName(), user.getBiography()));
            model.addAttribute("experienceHasErrors", true);
            return profileView(user, model);
        }
    }

    @PostMapping("/minha-conta/experiencias/{id}/remover")
    public String removeExperience(@PathVariable Long id, Authentication authentication,
                                   HttpServletRequest request, RedirectAttributes redirectAttributes) {
        AppUser user = currentUser(authentication);
        var experience = profileService.removeExperience(user, id);
        auditLogService.log(request, user, "PROFILE_EXPERIENCE_REMOVED", "CANDIDATE_EXPERIENCE",
                experience.getId(), "Experiência profissional removida do perfil.");
        redirectAttributes.addFlashAttribute("experienceRemoved", true);
        return "redirect:/minha-conta#experiencias";
    }

    @PostMapping("/minha-conta/tema")
    public ResponseEntity<Void> changeTheme(@RequestParam String theme, Authentication authentication,
                                            HttpServletRequest request) {
        AppUser user = currentUser(authentication);
        ThemePreference preference;
        try {
            preference = ThemePreference.valueOf(theme.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().build();
        }
        profileService.changeTheme(user, preference);
        auditLogService.log(request, user, "PROFILE_THEME_UPDATED", "APP_USER", user.getId(),
                "Preferência de tema alterada para " + preference.name() + ".");
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/minha-conta/foto")
    public ResponseEntity<byte[]> photo(Authentication authentication) {
        AppUser user = currentUser(authentication);
        if (user.getPhotoContent() == null || user.getPhotoContentType() == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(user.getPhotoContentType()))
                .cacheControl(CacheControl.maxAge(Duration.ofHours(12)).cachePrivate())
                .body(user.getPhotoContent());
    }

    @GetMapping("/minha-conta/curriculo")
    public ResponseEntity<byte[]> resume(Authentication authentication) {
        AppUser user = currentUser(authentication);
        if (user.getResumeContent() == null) {
            return ResponseEntity.notFound().build();
        }
        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(user.getResumeFileName(), StandardCharsets.UTF_8)
                .build();
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .body(user.getResumeContent());
    }

    private String profileView(AppUser user, Model model) {
        model.addAttribute("user", user);
        model.addAttribute("experiences", profileService.experiences(user));
        if (!model.containsAttribute("profileForm")) {
            model.addAttribute("profileForm", new ProfileUpdateForm(user.getName(), user.getBiography()));
        }
        if (!model.containsAttribute("experienceForm")) {
            model.addAttribute("experienceForm", new ExperienceForm(null, null, null, null, null));
        }
        return "auth/profile";
    }

    private AppUser currentUser(Authentication authentication) {
        return userService.currentUser(authentication).orElseThrow();
    }

    private boolean isAuthenticated(Authentication authentication) {
        return authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken);
    }
}
