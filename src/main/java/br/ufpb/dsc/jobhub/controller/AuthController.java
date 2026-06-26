package br.ufpb.dsc.jobhub.controller;

import br.ufpb.dsc.jobhub.domain.AppUser;
import br.ufpb.dsc.jobhub.dto.RegistrationForm;
import br.ufpb.dsc.jobhub.service.AuditLogService;
import br.ufpb.dsc.jobhub.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AuthController {

    private final UserService userService;
    private final AuditLogService auditLogService;

    public AuthController(UserService userService, AuditLogService auditLogService) {
        this.userService = userService;
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
        AppUser user = userService.currentUser(authentication).orElseThrow();
        model.addAttribute("user", user);
        return "auth/profile";
    }

    private boolean isAuthenticated(Authentication authentication) {
        return authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken);
    }
}
