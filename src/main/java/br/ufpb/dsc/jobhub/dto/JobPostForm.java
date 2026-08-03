package br.ufpb.dsc.jobhub.dto;

import br.ufpb.dsc.jobhub.domain.ContractType;
import br.ufpb.dsc.jobhub.domain.JobLocationType;
import br.ufpb.dsc.jobhub.domain.Seniority;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record JobPostForm(
        @NotBlank @Size(max = 160) String title,
        @NotBlank @Size(max = 140) String company,
        @NotBlank @Email @Size(max = 190) String companyEmail,
        @NotNull JobLocationType locationType,
        @Size(max = 80) String city,
        @NotNull Seniority seniority,
        @NotNull ContractType contractType,
        @Size(max = 80) String salaryRange,
        @NotBlank @Size(min = 40, max = 4000) String description,
        @Size(max = 4000) String requirements,
        @Size(max = 500) String applyUrl
) {
    public static JobPostForm empty() {
        return new JobPostForm("", "", "", JobLocationType.REMOTE, "", Seniority.JUNIOR,
                ContractType.CLT, "", "", "", "");
    }
}
