package net.devstudy.resume.ms.auth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

import net.devstudy.resume.ms.auth.config.AuthModuleWiringConfiguration;
import net.devstudy.resume.ms.auth.config.WebModuleWiringConfiguration;
import net.devstudy.resume.web.controller.SessionApiController;

@SpringBootApplication(scanBasePackageClasses = {
        AuthServiceApplication.class,
        SessionApiController.class
})
@Import({AuthModuleWiringConfiguration.class, WebModuleWiringConfiguration.class})
public class AuthServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AuthServiceApplication.class, args);
    }
}
