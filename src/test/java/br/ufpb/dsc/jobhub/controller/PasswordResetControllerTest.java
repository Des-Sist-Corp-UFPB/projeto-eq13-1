package br.ufpb.dsc.jobhub.controller;

import br.ufpb.dsc.jobhub.domain.AppUser;
import br.ufpb.dsc.jobhub.domain.AuthProvider;
import br.ufpb.dsc.jobhub.domain.UserRole;
import br.ufpb.dsc.jobhub.dto.ForgotPasswordForm;
import br.ufpb.dsc.jobhub.dto.PasswordResetForm;
import br.ufpb.dsc.jobhub.service.AuditLogService;
import br.ufpb.dsc.jobhub.service.PasswordResetService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.validation.BindingResult;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PasswordResetControllerTest {

    private PasswordResetService passwordResetService;
    private AuditLogService auditLogService;
    private PasswordResetController controller;
    private MockHttpServletRequest request;

    @BeforeEach
    void setUp() {
        passwordResetService = mock(PasswordResetService.class);
        auditLogService = mock(AuditLogService.class);
        controller = new PasswordResetController(passwordResetService, auditLogService);
        request = new MockHttpServletRequest();
    }

    @Test
    void rendersForgotPasswordFormAndPreservesExistingForm() {
        ExtendedModelMap model = new ExtendedModelMap();
        assertThat(controller.forgotPassword(model)).isEqualTo("auth/forgot-password");
        assertThat(model.get("form")).isEqualTo(ForgotPasswordForm.empty());

        ForgotPasswordForm existing = new ForgotPasswordForm("pessoa@example.com");
        model.addAttribute("form", existing);
        controller.forgotPassword(model);
        assertThat(model.get("form")).isSameAs(existing);
    }

    @Test
    void redisplaysInvalidEmailAndProcessesValidRequestGenerically() {
        BindingResult errors = mock(BindingResult.class);
        when(errors.hasErrors()).thenReturn(true);
        assertThat(controller.requestReset(new ForgotPasswordForm("invalido"), errors, request,
                new RedirectAttributesModelMap())).isEqualTo("auth/forgot-password");
        verify(passwordResetService, never()).requestReset("invalido");

        BindingResult valid = mock(BindingResult.class);
        RedirectAttributesModelMap redirect = new RedirectAttributesModelMap();
        assertThat(controller.requestReset(new ForgotPasswordForm("pessoa@example.com"), valid, request, redirect))
                .isEqualTo("redirect:/esqueci-senha");
        verify(passwordResetService).requestReset("pessoa@example.com");
        verify(auditLogService).log(request, "pessoa@example.com", "PASSWORD_RESET_REQUESTED", "AUTH", null,
                "Solicitação de recuperação de senha recebida.");
        assertThat(redirect.getFlashAttributes().get("requested")).isEqualTo(true);
    }

    @Test
    void rendersValidAndInvalidResetLinksAndPreservesExistingForm() {
        when(passwordResetService.isTokenValid("valid")).thenReturn(true);
        ExtendedModelMap model = new ExtendedModelMap();
        assertThat(controller.resetPassword("valid", model)).isEqualTo("auth/reset-password");
        assertThat(model).containsEntry("tokenValid", true);
        assertThat(((PasswordResetForm) model.get("form")).token()).isEqualTo("valid");

        PasswordResetForm existing = PasswordResetForm.forToken("existing");
        model.addAttribute("form", existing);
        when(passwordResetService.isTokenValid("expired")).thenReturn(false);
        controller.resetPassword("expired", model);
        assertThat(model).containsEntry("tokenValid", false);
        assertThat(model.get("form")).isSameAs(existing);
    }

    @Test
    void redisplaysResetFormForValidationMismatchAndInvalidToken() {
        PasswordResetForm form = new PasswordResetForm("token", "senha-segura", "senha-segura");
        BindingResult validationErrors = mock(BindingResult.class);
        when(validationErrors.hasErrors()).thenReturn(true);
        when(passwordResetService.isTokenValid("token")).thenReturn(true);
        ExtendedModelMap invalidModel = new ExtendedModelMap();
        assertThat(controller.completeReset(form, validationErrors, request, invalidModel,
                new RedirectAttributesModelMap())).isEqualTo("auth/reset-password");
        assertThat(invalidModel).containsEntry("tokenValid", true);

        PasswordResetForm mismatch = new PasswordResetForm("token", "senha-segura", "senha-diferente");
        BindingResult mismatchErrors = mock(BindingResult.class);
        ExtendedModelMap mismatchModel = new ExtendedModelMap();
        assertThat(controller.completeReset(mismatch, mismatchErrors, request, mismatchModel,
                new RedirectAttributesModelMap())).isEqualTo("auth/reset-password");
        verify(mismatchErrors).rejectValue("confirmPassword", "password.mismatch", "As senhas não coincidem.");
        assertThat(mismatchModel).containsEntry("tokenValid", true);

        BindingResult tokenErrors = mock(BindingResult.class);
        ExtendedModelMap tokenModel = new ExtendedModelMap();
        when(passwordResetService.resetPassword("token", "senha-segura", "senha-segura"))
                .thenReturn(Optional.empty());
        assertThat(controller.completeReset(form, tokenErrors, request, tokenModel,
                new RedirectAttributesModelMap())).isEqualTo("auth/reset-password");
        verify(tokenErrors).reject("token.invalid", "Este link é inválido, já foi utilizado ou expirou.");
        assertThat(tokenModel).containsEntry("tokenValid", false);
    }

    @Test
    void completesResetAuditsItAndReturnsToLogin() {
        AppUser user = new AppUser("Pessoa", "pessoa@example.com", "pessoa", "hash",
                UserRole.ROLE_USER, AuthProvider.LOCAL);
        PasswordResetForm form = new PasswordResetForm("token", "senha-segura", "senha-segura");
        when(passwordResetService.resetPassword("token", "senha-segura", "senha-segura"))
                .thenReturn(Optional.of(user));
        RedirectAttributesModelMap redirect = new RedirectAttributesModelMap();

        assertThat(controller.completeReset(form, mock(BindingResult.class), request,
                new ExtendedModelMap(), redirect)).isEqualTo("redirect:/login");
        verify(auditLogService).log(request, user, "PASSWORD_RESET_COMPLETED", "APP_USER", null,
                "Senha redefinida por link enviado ao e-mail cadastrado.");
        assertThat(redirect.getFlashAttributes().get("passwordReset")).isEqualTo(true);
    }
}
