package br.ufpb.dsc.jobhub.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PasswordResetForm(
        @NotBlank(message = "Token de recuperação ausente.") String token,
        @NotBlank(message = "Informe a nova senha.")
        @Size(min = 8, max = 72, message = "A senha deve ter entre 8 e 72 caracteres.") String password,
        @NotBlank(message = "Confirme a nova senha.") String confirmPassword
) {
    public static PasswordResetForm forToken(String token) {
        return new PasswordResetForm(token, "", "");
    }
}
