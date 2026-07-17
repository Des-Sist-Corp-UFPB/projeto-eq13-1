package br.ufpb.dsc.jobhub.controller;

import br.ufpb.dsc.jobhub.domain.AppUser;
import br.ufpb.dsc.jobhub.domain.ThemePreference;
import br.ufpb.dsc.jobhub.dto.RegistrationForm;
import br.ufpb.dsc.jobhub.repository.AuditLogRepository;
import br.ufpb.dsc.jobhub.repository.CandidateExperienceRepository;
import br.ufpb.dsc.jobhub.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CandidateProfileIntegrationTest {

    private static final String EMAIL = "perfil.integracao@example.com";

    @Autowired private MockMvc mockMvc;
    @Autowired private UserService userService;
    @Autowired private CandidateExperienceRepository experienceRepository;
    @Autowired private AuditLogRepository auditLogRepository;

    private AppUser user;

    @BeforeEach
    void setUp() {
        user = userService.findByLogin(EMAIL).orElseGet(() -> userService.registerLocal(
                new RegistrationForm("Perfil Integração", EMAIL, "perfilintegracao", "senha1234")
        ));
    }

    @Test
    void profileSubroutesRequireAuthenticationAndPublicLayoutContainsBrandAndTheme() throws Exception {
        mockMvc.perform(get("/minha-conta/foto")).andExpect(status().is3xxRedirection());
        mockMvc.perform(post("/minha-conta/tema").with(csrf()).param("theme", "dark"))
                .andExpect(status().is3xxRedirection());
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("radartech-logo-transparent.png")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("radartech-logo-dark.png")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("data-theme=\"light\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("theme-toggle")));
    }

    @Test
    void userUpdatesProfilePhotoResumeAndThemeWithAudit() throws Exception {
        byte[] png = new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0, 0, 0, 0, 0, 0, 0, 0};
        var photo = new MockMultipartFile("photo", "perfil.png", "image/png", png);
        var resume = new MockMultipartFile("resume", "curriculo.pdf", "application/pdf", "%PDF-1.7 perfil".getBytes());

        mockMvc.perform(multipart("/minha-conta")
                        .file(photo).file(resume).with(csrf()).with(authenticatedUser())
                        .param("name", "Perfil Atualizado")
                        .param("biography", "Desenvolvedor de software na Paraíba."))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/minha-conta"));

        mockMvc.perform(get("/minha-conta/foto").with(authenticatedUser()))
                .andExpect(status().isOk()).andExpect(content().contentType("image/png"));
        mockMvc.perform(get("/minha-conta/curriculo").with(authenticatedUser()))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/pdf"))
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("curriculo.pdf")));

        mockMvc.perform(post("/minha-conta/tema").with(csrf()).with(authenticatedUser()).param("theme", "dark"))
                .andExpect(status().isNoContent());

        AppUser updated = userService.findByLogin(EMAIL).orElseThrow();
        assertThat(updated.getName()).isEqualTo("Perfil Atualizado");
        assertThat(updated.getThemePreference()).isEqualTo(ThemePreference.DARK);
        assertThat(auditLogRepository.search("PROFILE_", EMAIL, "APP_USER", null, null)).hasSizeGreaterThanOrEqualTo(4);
    }

    @Test
    void invalidProfileFilesReturnTheProfilePage() throws Exception {
        var fakePdf = new MockMultipartFile("resume", "curriculo.pdf", "application/pdf", "texto".getBytes());
        mockMvc.perform(multipart("/minha-conta")
                        .file(fakePdf).with(csrf()).with(authenticatedUser())
                        .param("name", "Perfil Integração")
                        .param("biography", "Bio"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/profile"));

        mockMvc.perform(post("/minha-conta/tema").with(csrf()).with(authenticatedUser()).param("theme", "invalido"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void userAddsAndRemovesExperienceWithAudit() throws Exception {
        mockMvc.perform(post("/minha-conta/experiencias")
                        .with(csrf()).with(authenticatedUser())
                        .param("roleTitle", "Pessoa Desenvolvedora")
                        .param("company", "Radar Tech")
                        .param("startedOn", "2025-01-01")
                        .param("description", "APIs e interfaces web."))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/minha-conta#experiencias"));

        var experience = experienceRepository.findByUserIdOrderByStartedOnDesc(user.getId()).get(0);
        mockMvc.perform(post("/minha-conta/experiencias/{id}/remover", experience.getId())
                        .with(csrf()).with(authenticatedUser()))
                .andExpect(status().is3xxRedirection());
        assertThat(experienceRepository.findById(experience.getId())).isEmpty();
        assertThat(auditLogRepository.search("PROFILE_EXPERIENCE", EMAIL, "CANDIDATE_EXPERIENCE", null, null))
                .hasSizeGreaterThanOrEqualTo(2);
    }

    @Test
    void invalidExperienceReturnsProfileAndMissingAssetsReturnNotFound() throws Exception {
        mockMvc.perform(post("/minha-conta/experiencias")
                        .with(csrf()).with(authenticatedUser())
                        .param("roleTitle", "Dev")
                        .param("company", "Radar")
                        .param("startedOn", "2025-02-01")
                        .param("endedOn", "2025-01-01"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/profile"));

        AppUser withoutAssets = userService.registerLocal(new RegistrationForm(
                "Sem Arquivos", "sem.arquivos@example.com", "semarquivos", "senha1234"));
        var noAssets = SecurityMockMvcRequestPostProcessors.user(withoutAssets.getEmail()).roles("USER");
        mockMvc.perform(get("/minha-conta/foto").with(noAssets)).andExpect(status().isNotFound());
        mockMvc.perform(get("/minha-conta/curriculo").with(noAssets)).andExpect(status().isNotFound());
    }

    private SecurityMockMvcRequestPostProcessors.UserRequestPostProcessor authenticatedUser() {
        return SecurityMockMvcRequestPostProcessors.user(EMAIL).roles("USER");
    }
}
