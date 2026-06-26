package br.ufpb.dsc.jobhub.config;

import br.ufpb.dsc.jobhub.service.UserService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class AdminUserInitializer implements ApplicationRunner {

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
        userService.ensureAdminUser(username, password);
    }
}
