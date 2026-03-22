package net.devstudy.resume.profile.api.config;

import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "profile.hobbies")
public class ProfileHobbiesProperties {
    @Min(0)
    private int max = 5;

    public int getMax() {
        return max;
    }

    public void setMax(int max) {
        this.max = max;
    }
}
