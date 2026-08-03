package br.ufpb.dsc.jobhub.service;

import br.ufpb.dsc.jobhub.domain.AppUser;
import br.ufpb.dsc.jobhub.domain.AuthProvider;
import br.ufpb.dsc.jobhub.domain.CandidateExperience;
import br.ufpb.dsc.jobhub.domain.UserRole;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class AiCareerServiceTest {

    @Test
    void sendsProfessionalContextToOpenAiCompatibleLiteLlm() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        AiCareerService service = new AiCareerService(
                builder, "https://llm.example.test/", "test-api-key", "gpt-4o-mini", true);
        AppUser user = user();
        user.updateProfile("Ana", "Desenvolvedora Java em início de carreira.");
        CandidateExperience experience = new CandidateExperience(
                user, "Estagiária", "Radar Tech", LocalDate.of(2025, 1, 1), null, "APIs");

        server.expect(requestTo("https://llm.example.test/v1/chat/completions"))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer test-api-key"))
                .andRespond(withSuccess("""
                        {"choices":[{"message":{"role":"assistant","content":"Destaque seus projetos Java."}}],
                         "usage":{"total_tokens":42}}
                        """, MediaType.APPLICATION_JSON));

        assertThat(service.generateCareerAdvice(user, List.of(experience), "Como melhorar?"))
                .isEqualTo("Destaque seus projetos Java.");
        server.verify();
    }

    @Test
    void requiresEnabledIntegrationApiKeyAndQuestion() {
        AppUser user = user();
        AiCareerService disabled = new AiCareerService(
                RestClient.builder(), "https://llm.example.test", "key", "gpt-4o-mini", false);
        AiCareerService missingKey = new AiCareerService(
                RestClient.builder(), "https://llm.example.test", "", "gpt-4o-mini", true);
        AiCareerService enabled = new AiCareerService(
                RestClient.builder(), "https://llm.example.test", "key", "gpt-4o-mini", true);

        assertThatThrownBy(() -> disabled.generateCareerAdvice(user, List.of(), "Ajuda"))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("desativado");
        assertThatThrownBy(() -> missingKey.generateCareerAdvice(user, List.of(), "Ajuda"))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("LiteLLM");
        assertThatThrownBy(() -> enabled.generateCareerAdvice(user, List.of(), " "))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("vazia");
    }

    @Test
    void rejectsEmptyProviderResponse() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        AiCareerService service = new AiCareerService(
                builder, "https://llm.example.test", "test-key", "gpt-4o-mini", true);
        server.expect(requestTo("https://llm.example.test/v1/chat/completions"))
                .andRespond(withSuccess("{\"choices\":[]}", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> service.generateCareerAdvice(user(), List.of(), "Ajuda"))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("não retornou");
        server.verify();
    }

    private AppUser user() {
        return new AppUser("Ana", "ana@example.com", "ana", "hash", UserRole.ROLE_USER, AuthProvider.LOCAL);
    }
}
