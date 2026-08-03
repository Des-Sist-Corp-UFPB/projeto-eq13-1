package br.ufpb.dsc.jobhub;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.boot.SpringApplication;

import static org.mockito.Mockito.mockStatic;

class JobhubApplicationTest {

    @Test
    void delegatesStartupToSpringBoot() {
        String[] args = {"--spring.profiles.active=test"};

        try (MockedStatic<SpringApplication> spring = mockStatic(SpringApplication.class)) {
            JobhubApplication.main(args);
            spring.verify(() -> SpringApplication.run(JobhubApplication.class, args));
        }
    }
}
