package br.ufpb.dsc.jobhub.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AiCareerForm(
        @NotBlank(message = "Escreva uma pergunta para o assistente.")
        @Size(max = 800, message = "A pergunta deve ter no máximo 800 caracteres.")
        String question
) {
}
