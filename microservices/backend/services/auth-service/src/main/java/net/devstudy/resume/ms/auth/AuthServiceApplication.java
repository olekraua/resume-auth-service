package net.devstudy.resume.ms.auth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication(scanBasePackages = {
        "net.devstudy.resume.ms.auth",
        "net.devstudy.resume.auth",
        "net.devstudy.resume.web",
        "net.devstudy.resume.shared"
})
@EnableCaching
@ConfigurationPropertiesScan(basePackages = {
        "net.devstudy.resume.ms.auth",
        "net.devstudy.resume.auth",
        "net.devstudy.resume.web",
        "net.devstudy.resume.shared"
})
public class AuthServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AuthServiceApplication.class, args);
    }
}
