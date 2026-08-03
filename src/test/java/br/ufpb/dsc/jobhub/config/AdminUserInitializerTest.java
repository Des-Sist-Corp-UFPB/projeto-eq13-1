package br.ufpb.dsc.jobhub.config;

import br.ufpb.dsc.jobhub.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.ApplicationArguments;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class AdminUserInitializerTest {

    private final UserService userService = mock(UserService.class);
    private final ApplicationArguments arguments = mock(ApplicationArguments.class);

    @Test
    void createsOrRotatesAdminOnlyWithConfiguredPassword() {
        new AdminUserInitializer(userService, "admin", "senha-segura").run(arguments);

        verify(userService).ensureAdminUser("admin", "senha-segura");
    }

    @Test
    void skipsBootstrapWhenProductionSecretIsMissing() {
        new AdminUserInitializer(userService, "admin", " ").run(arguments);

        verify(userService, never()).ensureAdminUser(
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString());
    }
}
