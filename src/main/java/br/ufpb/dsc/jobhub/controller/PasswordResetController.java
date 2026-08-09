package br.ufpb.dsc.jobhub.controller;

import br.ufpb.dsc.jobhub.domain.AppUser;
import br.ufpb.dsc.jobhub.dto.ForgotPasswordForm;
import br.ufpb.dsc.jobhub.dto.PasswordResetForm;
import br.ufpb.dsc.jobhub.service.AuditLogService;
import br.ufpb.dsc.jobhub.service.PasswordResetService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Optional;

@Controller
public class PasswordResetController {

    private final PasswordResetService passwordResetService;
    private final AuditLogService auditLogService;

    public PasswordResetController(PasswordResetService passwordResetService, AuditLogService auditLogService) {
        this.passwordResetService = passwordResetService;
        this.auditLogService = auditLogService;
    }

    @GetMapping("/esqueci-senha")
    public String forgotPassword(Model model) {
        if (!model.containsAttribute("form")) {
            model.addAttribute("form", ForgotPasswordForm.empty());
        }
        return "auth/forgot-password";
    }

    @PostMapping("/esqueci-senha")
    public String requestReset(@Valid @ModelAttribute("form") ForgotPasswordForm form,
                               BindingResult bindingResult,
                               HttpServletRequest request,
                               RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return "auth/forgot-password";
        }
        passwordResetService.requestReset(form.email());
        auditLogService.log(request, form.email(), "PASSWORD_RESET_REQUESTED", "AUTH", null,
                "Solicitação de recuperação de senha recebida.");
        redirectAttributes.addFlashAttribute("requested", true);
        return "redirect:/esqueci-senha";
    }

    @GetMapping("/redefinir-senha")
    public String resetPassword(@RequestParam(required = false) String token, Model model) {
        model.addAttribute("tokenValid", passwordResetService.isTokenValid(token));
        if (!model.containsAttribute("form")) {
            model.addAttribute("form", PasswordResetForm.forToken(token));
        }
        return "auth/reset-password";
    }

    @PostMapping("/redefinir-senha")
    public String completeReset(@Valid @ModelAttribute("form") PasswordResetForm form,
                                BindingResult bindingResult,
                                HttpServletRequest request,
                                Model model,
                                RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("tokenValid", passwordResetService.isTokenValid(form.token()));
            return "auth/reset-password";
        }
        if (!form.password().equals(form.confirmPassword())) {
            bindingResult.rejectValue("confirmPassword", "password.mismatch", "As senhas não coincidem.");
            model.addAttribute("tokenValid", true);
            return "auth/reset-password";
        }

        Optional<AppUser> user = passwordResetService.resetPassword(
                form.token(), form.password(), form.confirmPassword());
        if (user.isEmpty()) {
            bindingResult.reject("token.invalid", "Este link é inválido, já foi utilizado ou expirou.");
            model.addAttribute("tokenValid", false);
            return "auth/reset-password";
        }

        auditLogService.log(request, user.get(), "PASSWORD_RESET_COMPLETED", "APP_USER", user.get().getId(),
                "Senha redefinida por link enviado ao e-mail cadastrado.");
        redirectAttributes.addFlashAttribute("passwordReset", true);
        return "redirect:/login";
    }
}
