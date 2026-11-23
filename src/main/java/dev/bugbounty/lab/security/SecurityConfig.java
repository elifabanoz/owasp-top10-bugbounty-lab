package dev.bugbounty.lab.security;

import dev.bugbounty.lab.config.LabMode;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;

@Configuration
public class SecurityConfig {
    @Bean
    SecurityFilterChain security(HttpSecurity http, LabMode mode, TokenFilter tokenFilter) throws Exception {
        http.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .httpBasic(basic -> basic.disable())
                .formLogin(form -> form.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/meta", "/api/auth/login", "/api/auth/csrf",
                                "/api/lab/debug/users", "/api/lab/webhook/triage").permitAll()
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .anyRequest().authenticated())
                .exceptionHandling(errors -> errors.authenticationEntryPoint((request, response, error) -> {
                    response.setStatus(401);
                    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                    response.getWriter().write("{\"error\":\"Authentication required\"}");
                }))
                .addFilterBefore(tokenFilter, UsernamePasswordAuthenticationFilter.class);
        if (mode.vulnerable()) {
            // intentionally vulnerable: cookie-authenticated writes have no CSRF check
            http.csrf(csrf -> csrf.disable());
        } else {
            var csrfRepository = CookieCsrfTokenRepository.withHttpOnlyFalse();
            csrfRepository.setCookieCustomizer(cookie -> cookie.secure(true).sameSite("Strict"));
            http.csrf(csrf -> csrf.csrfTokenRepository(csrfRepository)
                    .ignoringRequestMatchers("/api/auth/login", "/api/lab/webhook/triage"));
            http.headers(headers -> headers.contentSecurityPolicy(csp -> csp
                    .policyDirectives("default-src 'self'; script-src 'self'; object-src 'none'; base-uri 'none'; frame-ancestors 'none'")));
        }
        return http.build();
    }
}
