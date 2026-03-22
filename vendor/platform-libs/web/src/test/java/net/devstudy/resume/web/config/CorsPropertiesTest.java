package net.devstudy.resume.web.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

class CorsPropertiesTest {

    @Test
    void shouldSplitCommaSeparatedOriginsAndExpandLocalhostSchemes() {
        CorsProperties properties = new CorsProperties();

        properties.setAllowedOrigins(List.of(
                "https://localhost:4200, http://127.0.0.1:4201",
                "https://example.com"));

        assertThat(properties.getAllowedOrigins()).containsExactlyInAnyOrder(
                "https://localhost:4200",
                "http://localhost:4200",
                "http://127.0.0.1:4201",
                "https://127.0.0.1:4201",
                "https://example.com");
    }

    @Test
    void shouldIgnoreBlankOrigins() {
        CorsProperties properties = new CorsProperties();

        properties.setAllowedOrigins(List.of("  ", ",", " https://localhost:4200 "));

        assertThat(properties.getAllowedOrigins()).containsExactlyInAnyOrder(
                "https://localhost:4200",
                "http://localhost:4200");
    }

    @Test
    void shouldNotExpandNonLocalOrigins() {
        CorsProperties properties = new CorsProperties();

        properties.setAllowedOrigins(List.of("https://resume.devstudy.net"));

        assertThat(properties.getAllowedOrigins()).containsExactly("https://resume.devstudy.net");
    }
}
