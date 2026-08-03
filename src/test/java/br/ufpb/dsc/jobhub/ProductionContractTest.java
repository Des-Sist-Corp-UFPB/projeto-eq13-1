package br.ufpb.dsc.jobhub;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

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
}
