package br.ufpb.dsc.jobhub.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

public record ExperienceForm(
        @NotBlank(message = "Informe o cargo.")
        @Size(max = 140)
        String roleTitle,

        @NotBlank(message = "Informe a empresa.")
        @Size(max = 140)
        String company,

        @NotNull(message = "Informe a data de início.")
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate startedOn,

        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate endedOn,

        @Size(max = 1200, message = "A descrição deve ter no máximo 1.200 caracteres.")
        String description
) {
}
