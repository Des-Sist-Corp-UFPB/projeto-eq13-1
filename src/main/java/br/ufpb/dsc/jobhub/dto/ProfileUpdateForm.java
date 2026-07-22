package br.ufpb.dsc.jobhub.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ProfileUpdateForm(
        @NotBlank(message = "Informe seu nome.")
        @Size(max = 140, message = "O nome deve ter no máximo 140 caracteres.")
        String name,

        @Size(max = 1200, message = "A biografia deve ter no máximo 1.200 caracteres.")
        String biography
) {
}
