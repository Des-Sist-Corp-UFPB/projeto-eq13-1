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
                .contains("forward-headers-strategy: framework");
    }

    @Test
    void frontendUsesTheSharedRadarTechDesignSystemWithoutLegacyBranding() throws IOException {
        Path resources = Path.of("src", "main", "resources");
        String layout = Files.readString(resources.resolve(Path.of("templates", "fragments", "layout.html")));
        String home = Files.readString(resources.resolve(Path.of("templates", "home.html")));
        String appCss = Files.readString(resources.resolve(Path.of("static", "css", "app.css")));
        String homeCss = Files.readString(resources.resolve(Path.of("static", "css", "pages", "home.css")));

        assertThat(layout)
                .contains("Radar Tech")
                .contains("/css/app.css")
                .contains("type=\"module\"")
                .contains("umami.dsc.rodrigor.com/script.js")
                .contains("data-theme-choice=\"system\"")
                .doesNotContain("RadarTech PB", "JobHub PB");
        assertThat(home)
                .contains("name=\"q\"")
                .contains("name=\"location\"")
                .contains("fragments/job-card");
        assertThat(homeCss)
                .contains("radartech-hero-office.webp")
                .contains("[data-theme=\"dark\"] .hero")
                .contains("@media (max-width: 767px)");
        assertThat(appCss)
                .contains("@import url('./tokens.css')")
                .contains("@import url('./pages/home.css')")
                .contains("@import url('./pages/admin.css')");

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
}
