package br.ufpb.dsc.jobhub.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final AuditAuthenticationSuccessHandler authenticationSuccessHandler;
    private final AuditAuthenticationFailureHandler authenticationFailureHandler;
    private final AuditLogoutSuccessHandler logoutSuccessHandler;
    private final RadarOAuth2UserService oAuth2UserService;
    private final AuditOAuth2SuccessHandler oAuth2SuccessHandler;

    public SecurityConfig(AuditAuthenticationSuccessHandler authenticationSuccessHandler,
                          AuditAuthenticationFailureHandler authenticationFailureHandler,
                          AuditLogoutSuccessHandler logoutSuccessHandler,
                          RadarOAuth2UserService oAuth2UserService,
                          AuditOAuth2SuccessHandler oAuth2SuccessHandler) {
        this.authenticationSuccessHandler = authenticationSuccessHandler;
        this.authenticationFailureHandler = authenticationFailureHandler;
        this.logoutSuccessHandler = logoutSuccessHandler;
        this.oAuth2UserService = oAuth2UserService;
        this.oAuth2SuccessHandler = oAuth2SuccessHandler;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/", "/vagas", "/vagas/**", "/divulgar", "/ping", "/css/**", "/js/**", "/images/**", "/actuator/health").permitAll()
                        .requestMatchers("/login", "/cadastro", "/admin/login", "/oauth2/**", "/login/oauth2/**").permitAll()
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        .requestMatchers("/minha-conta/**").authenticated()
                        .anyRequest().permitAll()
                )
                .formLogin(login -> login
                        .loginPage("/login")
                        .loginProcessingUrl("/login")
                        .usernameParameter("username")
                        .successHandler(authenticationSuccessHandler)
                        .failureHandler(authenticationFailureHandler)
                        .permitAll()
                )
                .oauth2Login(oauth2 -> oauth2
                        .loginPage("/login")
                        .userInfoEndpoint(userInfo -> userInfo.userService(oAuth2UserService))
                        .successHandler(oAuth2SuccessHandler)
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutRequestMatcher(new OrRequestMatcher(
                                new AntPathRequestMatcher("/logout", "POST"),
                                new AntPathRequestMatcher("/admin/logout", "POST")
                        ))
                        .logoutSuccessHandler(logoutSuccessHandler)
                        .permitAll()
                );
        return http.build();
    }
}
