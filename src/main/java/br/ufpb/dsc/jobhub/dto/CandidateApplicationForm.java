package br.ufpb.dsc.jobhub.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CandidateApplicationForm(
        @NotBlank @Size(max = 140) String applicantName,
        @NotBlank @Email @Size(max = 190) String applicantEmail,
        @Size(max = 500) String linkedinUrl,
        @Size(max = 2000) String message
) {
    public static CandidateApplicationForm empty() {
        return new CandidateApplicationForm("", "", "", "");
    }
}
