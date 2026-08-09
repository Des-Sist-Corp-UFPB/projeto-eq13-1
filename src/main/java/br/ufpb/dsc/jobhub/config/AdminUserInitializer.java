package br.ufpb.dsc.jobhub.config;

import br.ufpb.dsc.jobhub.service.UserService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class AdminUserInitializer implements ApplicationRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(AdminUserInitializer.class);

    private final UserService userService;
    private final String username;
    private final String password;

    public AdminUserInitializer(UserService userService,
                                @Value("${app.admin.username:admin}") String username,
                                @Value("${app.admin.password:admin123}") String password) {
        this.userService = userService;
        this.username = username;
        this.password = password;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (password == null || password.isBlank()) {
            LOGGER.warn("ADMIN_PASSWORD não configurada; bootstrap e rotação do administrador foram ignorados.");
            return;
        }
        userService.ensureAdminUser(username, password);
    }
}
