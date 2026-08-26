package pl.szymtrener.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

@Configuration
public class SecurityConfig {

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/admin/logowanie", "/admin/logowanie/**").permitAll()
                .requestMatchers("/admin/**").hasRole("ADMIN")
                .anyRequest().permitAll())
            .formLogin(form -> form
                .loginPage("/admin/logowanie")
                .loginProcessingUrl("/admin/logowanie")
                .defaultSuccessUrl("/admin", true)
                .failureUrl("/admin/logowanie?blad")
                .permitAll())
            .logout(out -> out
                .logoutRequestMatcher(new AntPathRequestMatcher("/admin/wyloguj"))
                .logoutSuccessUrl("/admin/logowanie?wylogowano"))
            // CSRF zostaje wlaczony takze dla formularzy publicznych;
            // front dosyla token naglowkiem X-CSRF-TOKEN z <meta name="_csrf">.
            .headers(h -> h
                .frameOptions(f -> f.sameOrigin())
                .referrerPolicy(r -> r.policy(
                        org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter
                                .ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN)));
        return http.build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
