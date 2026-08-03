package br.ufpb.dsc.jobhub.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegistrationForm(
        @NotBlank @Size(max = 140) String name,
        @NotBlank @Email @Size(max = 190) String email,
        @Size(max = 80) String username,
        @NotBlank @Size(min = 8, max = 120) String password
) {
    public static RegistrationForm empty() {
        return new RegistrationForm("", "", "", "");
    }
}
