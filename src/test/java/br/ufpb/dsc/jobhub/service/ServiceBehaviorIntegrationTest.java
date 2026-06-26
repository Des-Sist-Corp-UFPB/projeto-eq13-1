package br.ufpb.dsc.jobhub.service;

import br.ufpb.dsc.jobhub.domain.AppUser;
import br.ufpb.dsc.jobhub.domain.AuditLog;
import br.ufpb.dsc.jobhub.domain.ContractType;
import br.ufpb.dsc.jobhub.domain.JobLocationType;
import br.ufpb.dsc.jobhub.domain.JobPosting;
import br.ufpb.dsc.jobhub.domain.JobStatus;
import br.ufpb.dsc.jobhub.domain.Seniority;
import br.ufpb.dsc.jobhub.domain.UserRole;
import br.ufpb.dsc.jobhub.dto.CandidateApplicationForm;
import br.ufpb.dsc.jobhub.dto.JobPostForm;
import br.ufpb.dsc.jobhub.dto.RegistrationForm;
import br.ufpb.dsc.jobhub.repository.AuditLogRepository;
import br.ufpb.dsc.jobhub.repository.JobPostingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static br.ufpb.dsc.jobhub.domain.ContractType.CLT;
import static br.ufpb.dsc.jobhub.domain.Seniority.JUNIOR;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("Serviços e repositórios")
class ServiceBehaviorIntegrationTest {

    @Autowired
    private UserService userService;

    @Autowired
    private JobService jobService;

    @Autowired
    private AuditLogService auditLogService;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private JobPostingRepository jobPostingRepository;

    @BeforeEach
    void seedRepositorySearchData() {
        if (jobPostingRepository.searchPublished("Zenvia", JobLocationType.REMOTE).isEmpty()) {
            JobPosting junior = new JobPosting(
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
            junior.publish();
            jobPostingRepository.save(junior);
        }
        if (jobPostingRepository.searchPublished("Estagio", JobLocationType.REMOTE).isEmpty()) {
            JobPosting internship = new JobPosting(
                    "Estagio em Desenvolvimento de Software",
                    "EdTech PB",
                    "estagios@edtechpb.dev",
                    JobLocationType.REMOTE,
                    null,
                    Seniority.INTERNSHIP,
                    ContractType.INTERNSHIP,
                    "Nao informado",
                    "Estagio remoto para estudantes atuarem em desenvolvimento e manutencao de sistemas.",
                    "Logica de programacao, Git, HTML, CSS e vontade de aprender.",
                    "https://example.com/estagio"
            );
            internship.publish();
            jobPostingRepository.save(internship);
        }
    }

    @Test
    void localUserRegistrationStoresBCryptPasswordAndRejectsDuplicateEmail() {
        RegistrationForm form = new RegistrationForm("João", "joao@example.com", "joao", "senha1234");
        AppUser user = userService.registerLocal(form);

        assertThat(user.getEmail()).isEqualTo("joao@example.com");
        assertThat(user.getPasswordHash()).startsWith("$2");

        assertThatThrownBy(() -> userService.registerLocal(form))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("e-mail");
    }

    @Test
    void googleUserIsCreatedOnceAndReused() {
        var created = userService.findOrCreateGoogleUser("google@example.com", "Google User");
        var reused = userService.findOrCreateGoogleUser("google@example.com", "Google User 2");

        assertThat(created.created()).isTrue();
        assertThat(reused.created()).isFalse();
        assertThat(reused.user().getId()).isEqualTo(created.user().getId());
    }

    @Test
    void userLookupAdminPromotionAndOAuthCurrentUserWork() {
        AppUser local = userService.registerLocal(new RegistrationForm(
                "Lookup User", "lookup.user@example.com", "lookupuser", "senha1234"
        ));

        var details = userService.loadUserByUsername("lookupuser");
        assertThat(details.getUsername()).isEqualTo(local.getEmail());
        assertThat(details.getAuthorities()).extracting("authority").contains("ROLE_USER");
        assertThat(userService.findByLogin(" ")).isEmpty();
        assertThatThrownBy(() -> userService.loadUserByUsername("ninguem@example.com"))
                .isInstanceOf(UsernameNotFoundException.class);

        AppUser createdAdmin = userService.ensureAdminUser("admincobertura", "admin1234");
        AppUser reusedAdmin = userService.ensureAdminUser("admincobertura", "admin5678");
        assertThat(reusedAdmin.getId()).isEqualTo(createdAdmin.getId());
        assertThat(reusedAdmin.getRole()).isEqualTo(UserRole.ROLE_ADMIN);

        var google = userService.findOrCreateGoogleUser("oauth-current@example.com", " ");
        var oauthUser = new DefaultOAuth2User(
                List.of(new SimpleGrantedAuthority("ROLE_USER")),
                Map.of("email", "oauth-current@example.com", "name", "OAuth Current"),
                "email"
        );
        var authentication = new TestingAuthenticationToken(oauthUser, "n/a", "ROLE_USER");

        assertThat(userService.currentUser(authentication))
                .isPresent()
                .get()
                .extracting(AppUser::getId)
                .isEqualTo(google.user().getId());
        assertThat(userService.currentUser(null)).isEmpty();
    }

    @Test
    void auditLogCanBePersistedAndFiltered() {
        auditLogService.logSystem("auditor@example.com", "TEST_ACTION", "TEST_ENTITY", 99L, "Evento de teste.");

        assertThat(auditLogRepository.search("TEST", "auditor", "TEST_ENTITY", null, null, org.springframework.data.domain.PageRequest.of(0, 20)))
                .hasSize(1);
        assertThat(auditLogService.search("TEST", "auditor", "TEST_ENTITY", LocalDate.now().minusDays(1), LocalDate.now().plusDays(1)))
                .hasSize(1);
    }

    @Test
    void auditLogCapturesRequestMetadataAndNullActors() {
        MockHttpServletRequest forwardedRequest = new MockHttpServletRequest();
        forwardedRequest.addHeader("X-Forwarded-For", "203.0.113.10, 10.0.0.20");
        forwardedRequest.addHeader("User-Agent", "JUnit");

        AuditLog anonymousLog = auditLogService.log(
                forwardedRequest, (AppUser) null, "ANON_ACTION", "TEST_ENTITY", null, "Evento anonimo."
        );
        assertThat(anonymousLog.getActorEmail()).isNull();
        assertThat(anonymousLog.getEntityId()).isNull();
        assertThat(anonymousLog.getIpAddress()).isEqualTo("203.0.113.10");
        assertThat(anonymousLog.getUserAgent()).isEqualTo("JUnit");

        AuditLog emailLog = auditLogService.log(
                null, "externo@example.com", "EMAIL_ACTION", "TEST_ENTITY", 7L, "Evento externo."
        );
        assertThat(emailLog.getActorEmail()).isEqualTo("externo@example.com");
        assertThat(emailLog.getIpAddress()).isNull();
        assertThat(emailLog.getUserAgent()).isNull();
    }

    @Test
    void jobRulesRequireCityForHybridOrPresentialAndNormalizeRemoteCity() {
        JobPostForm hybridWithoutCity = new JobPostForm(
                "Dev Junior", "Empresa", "rh@empresa.com", JobLocationType.HYBRID_PB, "",
                JUNIOR, CLT, "Não informado", "Descrição longa o suficiente para validar a criação da vaga.",
                "Java e Spring", "https://example.com"
        );

        assertThatThrownBy(() -> jobService.createPending(hybridWithoutCity))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cidade");

        JobPostForm remoteWithCity = new JobPostForm(
                "Dev Remoto", "Empresa", "rh2@empresa.com", JobLocationType.REMOTE, "João Pessoa",
                JUNIOR, CLT, "Não informado", "Descrição longa o suficiente para validar a criação da vaga.",
                "Java e Spring", "https://example.com/remoto"
        );

        var job = jobService.createPending(remoteWithCity);
        assertThat(job.getCity()).isNull();
        assertThat(job.getStatus()).isEqualTo(JobStatus.PENDING);
    }

    @Test
    void jobServiceCoversFailuresFiltersApplicationsAndDeletion() {
        JobPosting published = jobPostingRepository.searchPublished("Zenvia", JobLocationType.REMOTE).get(0);
        CandidateApplicationForm applicationForm = new CandidateApplicationForm(
                "  Candidato QA  ",
                "qa@example.com",
                "https://linkedin.com/in/qa",
                "  Quero participar.  "
        );

        var application = jobService.apply(published.getId(), applicationForm);
        assertThat(application.getApplicantName()).isEqualTo("Candidato QA");
        assertThatThrownBy(() -> jobService.publicDetails(-1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Vaga");
        assertThatThrownBy(() -> jobService.apply(-1L, applicationForm))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Vaga");

        assertThat(jobService.searchPublished(null, "location-invalida")).isNotEmpty();
        assertThat(jobService.searchAdmin(null, "status-invalido", "location-invalida")).isNotEmpty();

        JobPostForm hybridWithCity = new JobPostForm(
                "Dev Hibrido", "Empresa PB", "rh3@empresa.com", JobLocationType.HYBRID_PB, " Campina Grande ",
                JUNIOR, CLT, "Nao informado", "Descricao longa o suficiente para validar a criacao da vaga hibrida.",
                "Java e SQL", "https://example.com/hibrido"
        );
        JobPosting hybrid = jobService.createPending(hybridWithCity);

        assertThat(hybrid.getCity()).isEqualTo("Campina Grande");
        assertThat(jobService.findAny(hybrid.getId()).getId()).isEqualTo(hybrid.getId());
        assertThat(jobService.delete(hybrid.getId()).getTitle()).isEqualTo("Dev Hibrido");
        assertThat(jobPostingRepository.findById(hybrid.getId())).isEmpty();
    }

    @Test
    void repositoriesSearchByStatusLocationAndKeyword() {
        assertThat(jobPostingRepository.searchAdmin("Zenvia", JobStatus.PUBLISHED, JobLocationType.REMOTE)).isNotEmpty();
        assertThat(jobPostingRepository.searchPublished("Estagio", JobLocationType.REMOTE)).isNotEmpty();
    }
}
