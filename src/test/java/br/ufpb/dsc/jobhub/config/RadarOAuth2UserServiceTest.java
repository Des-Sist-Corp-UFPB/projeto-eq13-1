package br.ufpb.dsc.jobhub.config;

import br.ufpb.dsc.jobhub.domain.AppUser;
import br.ufpb.dsc.jobhub.domain.AuthProvider;
import br.ufpb.dsc.jobhub.domain.UserRole;
import br.ufpb.dsc.jobhub.dto.GoogleUserProvision;
import br.ufpb.dsc.jobhub.service.AuditLogService;
import br.ufpb.dsc.jobhub.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RadarOAuth2UserServiceTest {

    private final UserService userService = mock(UserService.class);
    private final AuditLogService auditLogService = mock(AuditLogService.class);
    private final OAuth2UserService<OAuth2UserRequest, OAuth2User> delegate = mock(OAuth2UserService.class);
    private final OAuth2UserRequest request = mock(OAuth2UserRequest.class);

    @Test
    void provisionsAndAuditsNewGoogleUserUsingSubjectAsPrincipalName() {
        OAuth2User googleUser = oauthUser(Map.of(
                "sub", "google-123",
                "email", "nova@example.com",
                "name", "Nova Pessoa"
        ));
        AppUser appUser = appUser("Nova Pessoa", "nova@example.com", UserRole.ROLE_USER);
        when(delegate.loadUser(request)).thenReturn(googleUser);
        when(userService.findOrCreateGoogleUser("nova@example.com", "Nova Pessoa"))
                .thenReturn(new GoogleUserProvision(appUser, true));

        OAuth2User result = service().loadUser(request);

        assertThat(result.getName()).isEqualTo("google-123");
        assertThat(result.getAuthorities()).extracting("authority").containsExactly("ROLE_USER");
        verify(auditLogService).logSystem("nova@example.com", "GOOGLE_REGISTER", "APP_USER", null,
                "Usuário criado via Google OAuth2.");
    }

    @Test
    void reusesExistingAdminAndFallsBackToEmailAsPrincipalName() {
        OAuth2User googleUser = oauthUser(Map.of("email", "admin@example.com", "name", "Administrador"));
        AppUser admin = appUser("Administrador", "admin@example.com", UserRole.ROLE_ADMIN);
        when(delegate.loadUser(request)).thenReturn(googleUser);
        when(userService.findOrCreateGoogleUser("admin@example.com", "Administrador"))
                .thenReturn(new GoogleUserProvision(admin, false));

        OAuth2User result = service().loadUser(request);

        assertThat(result.getName()).isEqualTo("admin@example.com");
        assertThat(result.getAuthorities()).extracting("authority").containsExactly("ROLE_ADMIN");
        verify(auditLogService, never()).logSystem(
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any());
    }

    @Test
    void acceptsMissingDisplayNameButRejectsMissingOrBlankEmail() {
        OAuth2User withoutName = oauthUser(Map.of("email", "semnome@example.com"));
        AppUser appUser = appUser("semnome@example.com", "semnome@example.com", UserRole.ROLE_USER);
        when(delegate.loadUser(request)).thenReturn(withoutName);
        when(userService.findOrCreateGoogleUser("semnome@example.com", null))
                .thenReturn(new GoogleUserProvision(appUser, false));

        assertThat(service().loadUser(request).getName()).isEqualTo("semnome@example.com");

        OAuth2User withoutEmail = oauthUser(Map.of("name", "Sem e-mail"));
        when(delegate.loadUser(request)).thenReturn(withoutEmail);
        assertThatThrownBy(() -> service().loadUser(request))
                .isInstanceOf(OAuth2AuthenticationException.class);

        OAuth2User blankEmail = oauthUser(Map.of("email", " "));
        when(delegate.loadUser(request)).thenReturn(blankEmail);
        assertThatThrownBy(() -> service().loadUser(request))
                .isInstanceOf(OAuth2AuthenticationException.class);
    }

    private RadarOAuth2UserService service() {
        return new RadarOAuth2UserService(userService, auditLogService, delegate);
    }

    private OAuth2User oauthUser(Map<String, Object> attributes) {
        OAuth2User user = mock(OAuth2User.class);
        when(user.getAttributes()).thenReturn(attributes);
        return user;
    }

    private AppUser appUser(String name, String email, UserRole role) {
        return new AppUser(name, email, email, "oauth2", role, AuthProvider.GOOGLE);
    }
}
