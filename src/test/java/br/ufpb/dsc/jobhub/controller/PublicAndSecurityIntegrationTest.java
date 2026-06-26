package br.ufpb.dsc.jobhub.controller;

import br.ufpb.dsc.jobhub.domain.JobPosting;
import br.ufpb.dsc.jobhub.domain.JobLocationType;
import br.ufpb.dsc.jobhub.domain.JobStatus;
import br.ufpb.dsc.jobhub.domain.ContractType;
import br.ufpb.dsc.jobhub.domain.Seniority;
import br.ufpb.dsc.jobhub.repository.CandidateApplicationRepository;
import br.ufpb.dsc.jobhub.repository.AppUserRepository;
import br.ufpb.dsc.jobhub.repository.JobPostingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Fluxos públicos e segurança")
class PublicAndSecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JobPostingRepository jobPostingRepository;

    @Autowired
    private CandidateApplicationRepository applicationRepository;

    @Autowired
    private AppUserRepository userRepository;

    @BeforeEach
    void seedPublishedJob() {
        if (jobPostingRepository.searchPublished("Zenvia", JobLocationType.REMOTE).isEmpty()) {
            JobPosting job = new JobPosting(
                    "Pessoa Desenvolvedora Full Stack Junior",
                    "Zenvia",
                    "curadoria@radartechpb.dev",
                    JobLocationType.REMOTE,
                    null,
                    Seniority.JUNIOR,
                    ContractType.CLT,
                    "Nao informado",
                    "Vaga remota para desenvolvimento full stack com foco em front-end e produto.",
                    "Java, Spring, React, SQL e boas praticas de desenvolvimento.",
                    "https://example.com/zenvia"
            );
            job.publish();
            jobPostingRepository.save(job);
        }
    }

    @Test
    void publicPagesShouldBeAccessibleWithoutLogin() throws Exception {
        mockMvc.perform(get("/")).andExpect(status().isOk()).andExpect(view().name("home"));
        mockMvc.perform(get("/vagas")).andExpect(status().isOk()).andExpect(view().name("jobs/list"));
        mockMvc.perform(get("/divulgar")).andExpect(status().isOk()).andExpect(view().name("jobs/post"));
        mockMvc.perform(get("/login")).andExpect(status().isOk()).andExpect(view().name("auth/login"));
        mockMvc.perform(get("/cadastro")).andExpect(status().isOk()).andExpect(view().name("auth/register"));
    }

    @Test
    void loginRedirectsAuthenticatedUsersByRole() throws Exception {
        mockMvc.perform(get("/login").with(user("aluno@example.com").roles("USER")))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/minha-conta"));

        mockMvc.perform(get("/login").with(user("admin@radartech.local").roles("ADMIN")))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin"));
    }

    @Test
    void userCanRegisterAndAccessProfileWhenAuthenticated() throws Exception {
        mockMvc.perform(post("/cadastro")
                        .with(csrf())
                        .param("name", "Ana Souza")
                        .param("email", "ana.register@example.com")
                        .param("username", "anasouza")
                        .param("password", "senha1234"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));

        assertThat(userRepository.findByEmailIgnoreCase("ana.register@example.com")).isPresent();

        mockMvc.perform(get("/minha-conta"))
                .andExpect(status().is3xxRedirection());

        mockMvc.perform(get("/minha-conta")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("ana.register@example.com").roles("USER")))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/profile"));
    }

    @Test
    void registrationErrorsReturnRegisterPage() throws Exception {
        mockMvc.perform(post("/cadastro")
                        .with(csrf())
                        .param("name", "")
                        .param("email", "email-invalido")
                        .param("username", "usuario-invalido")
                        .param("password", "curta"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/register"));

        mockMvc.perform(post("/cadastro")
                        .with(csrf())
                        .param("name", "Usuario Um")
                        .param("email", "usuario.um@example.com")
                        .param("username", "usuario.duplicado")
                        .param("password", "senha1234"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));

        mockMvc.perform(post("/cadastro")
                        .with(csrf())
                        .param("name", "Usuario Dois")
                        .param("email", "usuario.dois@example.com")
                        .param("username", "usuario.duplicado")
                        .param("password", "senha1234"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/register"));
    }

    @Test
    void pingShouldBePublic() throws Exception {
        mockMvc.perform(get("/ping"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ok"))
                .andExpect(jsonPath("$.service").value("eq13"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void jobsCanBeFilteredAndViewed() throws Exception {
        JobPosting job = jobPostingRepository.searchPublished("", null).get(0);

        mockMvc.perform(get("/vagas").param("q", "estágio"))
                .andExpect(status().isOk())
                .andExpect(view().name("jobs/list"));

        mockMvc.perform(get("/vagas").param("location", "REMOTE"))
                .andExpect(status().isOk())
                .andExpect(view().name("jobs/list"));

        mockMvc.perform(get("/vagas/{id}", job.getId()))
                .andExpect(status().isOk())
                .andExpect(view().name("jobs/detail"));

        mockMvc.perform(get("/vagas/{id}", job.getId()).param("applied", "true"))
                .andExpect(status().isOk())
                .andExpect(view().name("jobs/detail"));
    }

    @Test
    void publicJobSubmissionCreatesPendingJob() throws Exception {
        mockMvc.perform(post("/divulgar")
                        .with(csrf())
                        .param("title", "Estágio em QA")
                        .param("company", "Teste Tech")
                        .param("companyEmail", "rh@testetech.com")
                        .param("locationType", "REMOTE")
                        .param("seniority", "INTERNSHIP")
                        .param("contractType", "INTERNSHIP")
                        .param("description", "Vaga remota para estudantes atuarem com testes e qualidade de software.")
                        .param("requirements", "Lógica, atenção a detalhes e vontade de aprender.")
                        .param("applyUrl", "https://example.com/qa"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/divulgar"));

        assertThat(jobPostingRepository.searchAdmin("Estágio em QA", JobStatus.PENDING, JobLocationType.REMOTE)).hasSize(1);
    }

    @Test
    void invalidPublicJobSubmissionReturnsForm() throws Exception {
        mockMvc.perform(post("/divulgar").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("jobs/post"));

        mockMvc.perform(post("/divulgar")
                        .with(csrf())
                        .param("title", "Estagio em Suporte")
                        .param("company", "Suporte PB")
                        .param("companyEmail", "rh@suportepb.com")
                        .param("locationType", "HYBRID_PB")
                        .param("seniority", "INTERNSHIP")
                        .param("contractType", "INTERNSHIP")
                        .param("description", "Vaga hibrida para estudantes acompanharem suporte tecnico e melhorias de sistemas.")
                        .param("requirements", "Boa comunicacao e logica.")
                        .param("applyUrl", "https://example.com/suporte"))
                .andExpect(status().isOk())
                .andExpect(view().name("jobs/post"));
    }

    @Test
    void internalApplicationCanBeSubmitted() throws Exception {
        JobPosting job = jobPostingRepository.searchPublished("", null).get(0);
        long before = applicationRepository.count();

        mockMvc.perform(post("/vagas/{id}/candidatar", job.getId())
                        .with(csrf())
                        .param("applicantName", "Maria Silva")
                        .param("applicantEmail", "maria@example.com")
                        .param("linkedinUrl", "https://linkedin.com/in/maria")
                        .param("message", "Tenho interesse na vaga."))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/vagas/" + job.getId() + "?applied=true"));

        assertThat(applicationRepository.count()).isEqualTo(before + 1);
    }

    @Test
    void invalidInternalApplicationReturnsDetailPage() throws Exception {
        JobPosting job = jobPostingRepository.searchPublished("", null).get(0);

        mockMvc.perform(post("/vagas/{id}/candidatar", job.getId())
                        .with(csrf())
                        .param("applicantName", "")
                        .param("applicantEmail", "email-invalido")
                        .param("linkedinUrl", "")
                        .param("message", ""))
                .andExpect(status().isOk())
                .andExpect(view().name("jobs/detail"));
    }

    @Test
    void adminAreaRequiresAdminRole() throws Exception {
        mockMvc.perform(get("/admin"))
                .andExpect(status().is3xxRedirection());

        mockMvc.perform(get("/admin").with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("user@example.com").roles("USER")))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/admin/auditoria").with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("user@example.com").roles("USER")))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/admin").with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("admin@radartech.local").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/dashboard"));
    }

    @Test
    @WithMockUser(username = "admin@radartech.local", roles = "ADMIN")
    void adminCanChangeJobStatusAndAccessAudit() throws Exception {
        mockMvc.perform(get("/admin/login"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));

        mockMvc.perform(get("/admin/vagas").param("status", "PUBLISHED").param("location", "REMOTE"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/jobs"));
        mockMvc.perform(get("/admin/vagas/nova"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/job-form"));
        mockMvc.perform(get("/admin/candidaturas"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/applications"));
        mockMvc.perform(get("/admin/usuarios"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/users"));

        mockMvc.perform(post("/admin/vagas/nova")
                        .with(csrf())
                        .param("title", "Analista Junior")
                        .param("company", "Admin PB")
                        .param("companyEmail", "rh@adminpb.com")
                        .param("locationType", "PRESENTIAL_PB")
                        .param("city", "Joao Pessoa")
                        .param("seniority", "JUNIOR")
                        .param("contractType", "CLT")
                        .param("salaryRange", "A combinar")
                        .param("description", "Vaga presencial na Paraiba para atuar com suporte e desenvolvimento de sistemas internos.")
                        .param("requirements", "Java, SQL e boa comunicacao.")
                        .param("applyUrl", "https://example.com/adminpb"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/vagas"));
        assertThat(jobPostingRepository.searchAdmin("Analista Junior", JobStatus.PUBLISHED, JobLocationType.PRESENTIAL_PB)).hasSize(1);

        mockMvc.perform(post("/admin/vagas/nova").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/job-form"));

        mockMvc.perform(post("/admin/vagas/nova")
                        .with(csrf())
                        .param("title", "Analista Hibrido")
                        .param("company", "Admin PB")
                        .param("companyEmail", "rh@adminpb.com")
                        .param("locationType", "HYBRID_PB")
                        .param("seniority", "JUNIOR")
                        .param("contractType", "CLT")
                        .param("description", "Vaga hibrida para atuar com desenvolvimento e suporte de sistemas internos.")
                        .param("requirements", "Java, SQL e comunicacao.")
                        .param("applyUrl", "https://example.com/adminpb-hibrido"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/job-form"));

        JobPosting pending = jobPostingRepository.save(new JobPosting(
                "Suporte Júnior",
                "Admin Tech",
                "rh@admintech.com",
                JobLocationType.REMOTE,
                null,
                Seniority.JUNIOR,
                ContractType.CLT,
                "Não informado",
                "Vaga remota para suporte de sistemas e atendimento técnico inicial.",
                "Comunicação, lógica e vontade de aprender.",
                null
        ));

        mockMvc.perform(post("/admin/vagas/{id}/publicar", pending.getId()).with(csrf()))
                .andExpect(status().is3xxRedirection());
        assertThat(jobPostingRepository.findById(pending.getId()).orElseThrow().getStatus()).isEqualTo(JobStatus.PUBLISHED);

        mockMvc.perform(post("/admin/vagas/{id}/pendente", pending.getId()).with(csrf()))
                .andExpect(status().is3xxRedirection());
        assertThat(jobPostingRepository.findById(pending.getId()).orElseThrow().getStatus()).isEqualTo(JobStatus.PENDING);

        mockMvc.perform(post("/admin/vagas/{id}/arquivar", pending.getId()).with(csrf()))
                .andExpect(status().is3xxRedirection());
        assertThat(jobPostingRepository.findById(pending.getId()).orElseThrow().getStatus()).isEqualTo(JobStatus.ARCHIVED);

        JobPosting removable = jobPostingRepository.save(new JobPosting(
                "Vaga para Remover",
                "Admin Delete",
                "rh@admindelete.com",
                JobLocationType.REMOTE,
                null,
                Seniority.JUNIOR,
                ContractType.CLT,
                "Nao informado",
                "Vaga criada apenas para validar a acao administrativa de remocao.",
                "Java e testes.",
                null
        ));

        mockMvc.perform(post("/admin/vagas/{id}/remover", removable.getId()).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/vagas"));
        assertThat(jobPostingRepository.findById(removable.getId())).isEmpty();

        mockMvc.perform(get("/admin/auditoria"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/audit"));

        mockMvc.perform(get("/admin/audit"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/auditoria"));
    }
}
