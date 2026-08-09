package br.ufpb.dsc.jobhub;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ProductionContractTest {

    @Test
    void containerAlwaysStartsWithProductionProfileAndPublicGoogleCallback() throws IOException {
        String dockerfile = Files.readString(Path.of("docker", "Dockerfile"));
        String productionConfig = Files.readString(Path.of(
                "src", "main", "resources", "application-prod.yml"));

        assertThat(dockerfile).contains("ENV SPRING_PROFILES_ACTIVE=prod");
        assertThat(productionConfig)
                .contains("https://eq13.dsc.rodrigor.com/login/oauth2/code/google")
                .contains("APP_PUBLIC_URL:https://eq13.dsc.rodrigor.com")
                .contains("forward-headers-strategy: framework");
    }

    @Test
    void frontendUsesTheSharedRadarTechDesignSystemWithoutLegacyBranding() throws IOException {
        Path resources = Path.of("src", "main", "resources");
        String layout = Files.readString(resources.resolve(Path.of("templates", "fragments", "layout.html")));
        String home = Files.readString(resources.resolve(Path.of("templates", "home.html")));
        String appCss = Files.readString(resources.resolve(Path.of("static", "css", "app.css")));
        String homeCss = Files.readString(resources.resolve(Path.of("static", "css", "pages", "home.css")));
        String jobsCss = Files.readString(resources.resolve(Path.of("static", "css", "pages", "jobs.css")));
        String jobDetail = Files.readString(resources.resolve(Path.of("templates", "jobs", "detail.html")));
        String profile = Files.readString(resources.resolve(Path.of("templates", "auth", "profile.html")));
        String profileMediaJs = Files.readString(resources.resolve(Path.of("static", "js", "modules", "profile-media.js")));

        assertThat(layout)
                .contains("Radar Tech")
                .contains("rel=\"icon\"")
                .contains("rel=\"apple-touch-icon\"")
                .contains("radartech-logo-transparent.png")
                .contains("/css/app.css")
                .contains("type=\"module\"")
                .contains("umami.dsc.rodrigor.com/script.js")
                .contains("class=\"theme-toggle\"")
                .contains("theme-icon-light")
                .contains("theme-icon-dark")
                .doesNotContain("data-theme-choice", "theme-menu-panel", "theme-icon-system", "Automático")
                .doesNotContain("RadarTech PB", "JobHub PB");
        assertThat(home)
                .contains("name=\"q\"")
                .contains("name=\"location\"")
                .contains("fragments/job-card");
        assertThat(homeCss)
                .contains("radartech-hero-office.webp")
                .contains("[data-theme=\"dark\"] .hero")
                .contains(".hero-search-field select option")
                .contains("width: min(780px, calc(100vw - 48px))")
                .contains(".hero-search-field:first-of-type")
                .contains(".hero-search-field:nth-of-type(2)")
                .contains("#c3d1dc")
                .contains("@media (max-width: 767px)");
        assertThat(jobDetail)
                .contains("Outras oportunidades")
                .contains("th:each=\"relatedJob : ${relatedJobs}\"")
                .contains("@{/vagas/{id}(id=${relatedJob.id})}")
                .doesNotContain("Abrir vaga original", "<h2>Candidatura externa</h2>");
        assertThat(jobsCss)
                .contains(".related-jobs__list")
                .contains(".related-job:hover")
                .contains(".related-job:focus-visible");
        assertThat(profile)
                .contains("data-media-open=\"photo\"")
                .contains("data-media-open=\"cover\"")
                .contains("/minha-conta/capa")
                .contains("data-position-x", "data-position-y")
                .doesNotContain("<strong data-file-name>Foto do perfil</strong>");
        assertThat(profileMediaJs)
                .contains("showModal")
                .contains("objectPosition")
                .contains("URL.createObjectURL");
        assertThat(appCss)
                .contains("@import url('./tokens.css')")
                .contains("@import url('./pages/home.css')")
                .contains("@import url('./pages/admin.css')");

        String themeJs = Files.readString(resources.resolve(Path.of("static", "js", "modules", "theme.js")));
        assertThat(themeJs)
                .contains("nextTheme")
                .contains("Ativar tema claro")
                .contains("Ativar tema escuro")
                .contains("keepalive: true")
                .contains("window.addEventListener('storage'")
                .doesNotContain("theme-menu-panel", "data-theme-choice", "Tema automático");

        List<Path> templates;
        try (var paths = Files.walk(resources.resolve("templates"))) {
            templates = paths.filter(path -> path.toString().endsWith(".html")).toList();
        }
        for (Path template : templates) {
            String templateContent = Files.readString(template);
            assertThat(templateContent)
                    .as("template %s", template)
                    .doesNotContain("RadarTech PB", "JobHub PB", "style=")
                    .doesNotContainPattern("(?s)<[^>]+th:(?:if|unless)=\"[^\"]+\"[^>]+th:replace=");
        }

        assertThat(resources.resolve(Path.of("static", "images", "radartech-logo-transparent.png"))).exists();
        assertThat(resources.resolve(Path.of("static", "images", "radartech-logo-dark.png"))).exists();
        assertThat(resources.resolve(Path.of("static", "images", "radartech-hero-office.webp"))).exists();
        assertThat(resources.resolve(Path.of("static", "images", "radartech-hero-office-mobile.webp"))).exists();
    }

    @Test
    void curatedCatalogKeepsOnlyVerifiedAugustWindowPublishedAndAudited() throws IOException {
        String migration = Files.readString(Path.of(
                "src", "main", "resources", "db", "migration", "V13__refresh_verified_tech_jobs_august.sql"));

        assertThat(migration)
                .contains("2026-08-03")
                .contains("2026-08-08")
                .contains("status = 'ARCHIVED'")
                .contains("'PUBLISHED'")
                .contains("linkedin.com/jobs/view")
                .contains("gupy.io/job")
                .contains("inhire.app/vagas")
                .contains("JOB_CATALOG_ARCHIVED")
                .contains("JOB_CURATED");
        assertThat(migration.split("https://", -1).length - 1).isGreaterThanOrEqualTo(20);
    }

    @Test
    void passwordResetUsesHashedExpiringTokensAndExternalMailConfiguration() throws IOException {
        Path resources = Path.of("src", "main", "resources");
        String migration = Files.readString(resources.resolve(Path.of(
                "db", "migration", "V14__create_password_reset_tokens.sql")));
        String application = Files.readString(resources.resolve("application.yml"));
        String login = Files.readString(resources.resolve(Path.of("templates", "auth", "login.html")));

        assertThat(migration)
                .contains("password_reset_token")
                .contains("token_hash varchar(64)")
                .contains("expires_at timestamptz")
                .contains("used_at timestamptz")
                .contains("references app_user(id)");
        assertThat(application)
                .contains("PASSWORD_RESET_EMAIL_ENABLED")
                .contains("MAIL_HOST")
                .contains("MAIL_PASSWORD")
                .contains("PASSWORD_RESET_TTL_MINUTES");
        assertThat(login).contains("/esqueci-senha", "Esqueceu sua senha?");
    }
}
