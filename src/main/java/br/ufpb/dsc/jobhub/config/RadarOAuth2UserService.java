package br.ufpb.dsc.jobhub.config;

import br.ufpb.dsc.jobhub.domain.AppUser;
import br.ufpb.dsc.jobhub.dto.GoogleUserProvision;
import br.ufpb.dsc.jobhub.service.AuditLogService;
import br.ufpb.dsc.jobhub.service.UserService;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class RadarOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

    private final DefaultOAuth2UserService delegate = new DefaultOAuth2UserService();
    private final UserService userService;
    private final AuditLogService auditLogService;

    public RadarOAuth2UserService(UserService userService, AuditLogService auditLogService) {
        this.userService = userService;
        this.auditLogService = auditLogService;
    }

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oauthUser = delegate.loadUser(userRequest);
        Map<String, Object> attributes = oauthUser.getAttributes();
        String email = value(attributes, "email");
        if (email == null || email.isBlank()) {
            throw new OAuth2AuthenticationException("Google não retornou um e-mail para autenticação.");
        }
        GoogleUserProvision provision = userService.findOrCreateGoogleUser(email, value(attributes, "name"));
        AppUser appUser = provision.user();
        if (provision.created()) {
            auditLogService.logSystem(appUser.getEmail(), "GOOGLE_REGISTER", "APP_USER", appUser.getId(), "Usuário criado via Google OAuth2.");
        }
        return new DefaultOAuth2User(
                List.of(new SimpleGrantedAuthority(appUser.getRole().name())),
                attributes,
                attributes.containsKey("sub") ? "sub" : "email"
        );
    }

    private String value(Map<String, Object> attributes, String key) {
        Object value = attributes.get(key);
        return value == null ? null : value.toString();
    }
}
