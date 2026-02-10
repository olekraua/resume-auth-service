package net.devstudy.resume.ms.auth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;

import net.devstudy.resume.web.controller.SessionApiController;
import net.devstudy.resume.web.controller.api.AccountApiController;
import net.devstudy.resume.web.controller.api.AuthApiController;
import net.devstudy.resume.web.controller.api.CsrfApiController;
import net.devstudy.resume.web.controller.api.PublicAuthApiController;

@SpringBootApplication
@EnableCaching
@ConfigurationPropertiesScan(basePackages = "net.devstudy.resume")
@ComponentScan(basePackages = {
        "net.devstudy.resume.auth",
        "net.devstudy.resume.notification",
        "net.devstudy.resume.ms.auth",
        "net.devstudy.resume.shared",
        "net.devstudy.resume.web.security",
        "net.devstudy.resume.web.config"
})
@Import({
        AuthApiController.class,
        PublicAuthApiController.class,
        AccountApiController.class,
        SessionApiController.class,
        CsrfApiController.class
})
public class AuthServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AuthServiceApplication.class, args);
    }
}
