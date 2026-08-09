package br.ufpb.dsc.jobhub.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ForgotPasswordForm(
        @NotBlank(message = "Informe o e-mail cadastrado.")
        @Email(message = "Informe um e-mail válido.")
        @Size(max = 190, message = "O e-mail deve ter no máximo 190 caracteres.")
        String email
) {
    public static ForgotPasswordForm empty() {
        return new ForgotPasswordForm("");
    }
}
