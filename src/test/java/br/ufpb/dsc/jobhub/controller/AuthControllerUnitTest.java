package br.ufpb.dsc.jobhub.controller;

import br.ufpb.dsc.jobhub.domain.AppUser;
import br.ufpb.dsc.jobhub.domain.AuthProvider;
import br.ufpb.dsc.jobhub.domain.UserRole;
import br.ufpb.dsc.jobhub.dto.AiCareerForm;
import br.ufpb.dsc.jobhub.dto.ExperienceForm;
import br.ufpb.dsc.jobhub.dto.ProfileUpdateForm;
import br.ufpb.dsc.jobhub.service.AiCareerService;
import br.ufpb.dsc.jobhub.service.AuditLogService;
import br.ufpb.dsc.jobhub.service.CandidateProfileService;
import br.ufpb.dsc.jobhub.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.validation.BindingResult;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AuthControllerUnitTest {

    private final UserService userService = mock(UserService.class);
    private final CandidateProfileService profileService = mock(CandidateProfileService.class);
    private final AuditLogService auditLogService = mock(AuditLogService.class);
    private final AiCareerService aiCareerService = mock(AiCareerService.class);
    private final Authentication authentication = mock(Authentication.class);
    private final MockHttpServletRequest request = new MockHttpServletRequest();
    private AuthController controller;

    @BeforeEach
    void setUp() {
        controller = new AuthController(userService, profileService, auditLogService, aiCareerService);
        when(userService.currentUser(authentication)).thenReturn(Optional.empty());
    }

    @Test
    void everyAccountResourceRejectsMissingAuthenticatedUser() {
        when(authentication.isAuthenticated()).thenReturn(true);
                doReturn(List.of(new SimpleGrantedAuthority("ROLE_USER"))).when(authentication).getAuthorities();

        assertThat(controller.login(authentication)).isEqualTo("redirect:/minha-conta");
        assertThat(controller.profile(authentication, new ExtendedModelMap()))
                .isEqualTo("redirect:/login?error");
        assertThat(controller.updateProfile(new ProfileUpdateForm("Pessoa", null), mock(BindingResult.class),
                null, null, authentication, request, new ExtendedModelMap(), new RedirectAttributesModelMap()))
                .isEqualTo("redirect:/login?error");
        assertThat(controller.addExperience(experience(), mock(BindingResult.class), authentication, request,
                new ExtendedModelMap(), new RedirectAttributesModelMap()))
                .isEqualTo("redirect:/login?error");
        assertThat(controller.removeExperience(1L, authentication, request, new RedirectAttributesModelMap()))
                .isEqualTo("redirect:/login?error");
        assertThat(controller.changeTheme("dark", authentication, request).getStatusCode().value()).isEqualTo(401);
        assertThat(controller.updatePhoto(null, 50, 50, authentication, request,
                new RedirectAttributesModelMap())).isEqualTo("redirect:/login?error");
        assertThat(controller.updateCover(null, 50, 50, authentication, request,
                new RedirectAttributesModelMap())).isEqualTo("redirect:/login?error");
        assertThat(controller.careerAssistant(new AiCareerForm("Como evoluir?"), mock(BindingResult.class),
                authentication, request, new RedirectAttributesModelMap()))
                .isEqualTo("redirect:/login?error");
        assertThat(controller.photo(authentication).getStatusCode().value()).isEqualTo(401);
        assertThat(controller.cover(authentication).getStatusCode().value()).isEqualTo(401);
        assertThat(controller.resume(authentication).getStatusCode().value()).isEqualTo(401);
    }

        @Test
        void oauth2AuthenticatedUsersWithoutResolvedAppUserStayOnLoginPage() {
                OAuth2User oauth2User = mock(OAuth2User.class);
                when(authentication.isAuthenticated()).thenReturn(true);
                when(authentication.getPrincipal()).thenReturn(oauth2User);
                doReturn(List.of(new SimpleGrantedAuthority("ROLE_USER"))).when(authentication).getAuthorities();

                assertThat(controller.login(authentication)).isEqualTo("auth/login");
        }

    @Test
    void redisplaysAccountFormsWithValidationErrorsAndProfileContext() {
        AppUser user = new AppUser("Pessoa", "pessoa@example.com", "pessoa", "hash",
                UserRole.ROLE_USER, AuthProvider.LOCAL);
        when(userService.currentUser(authentication)).thenReturn(Optional.of(user));
        when(profileService.experiences(user)).thenReturn(List.of());

        BindingResult profileErrors = mock(BindingResult.class);
        when(profileErrors.hasErrors()).thenReturn(true);
        ExtendedModelMap profileModel = new ExtendedModelMap();
        assertThat(controller.updateProfile(new ProfileUpdateForm("", null), profileErrors, null, null,
                authentication, request, profileModel, new RedirectAttributesModelMap()))
                .isEqualTo("auth/profile");
        assertThat(profileModel).containsKeys("user", "experiences", "profileForm", "experienceForm", "aiCareerForm");

        BindingResult experienceErrors = mock(BindingResult.class);
        when(experienceErrors.hasErrors()).thenReturn(true);
        ExtendedModelMap experienceModel = new ExtendedModelMap();
        assertThat(controller.addExperience(experience(), experienceErrors, authentication, request,
                experienceModel, new RedirectAttributesModelMap())).isEqualTo("auth/profile");
        assertThat(experienceModel).containsEntry("experienceHasErrors", true);
    }

    private ExperienceForm experience() {
        return new ExperienceForm("Desenvolvedor", "RadarTech", LocalDate.of(2025, 1, 1),
                null, "Atuação com produto.");
    }
}
