package pl.szymtrener.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    // Zasoby statyczne obsluguje domyslny handler Spring Boota
    // (naglowki cache ustawione w application.yml -> spring.web.resources.cache).
    // Na produkcji /css, /js, /images powinien serwowac nginx przed aplikacja.
}
