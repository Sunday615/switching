package com.example.switching.security.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.example.switching.security.filter.ApiKeyAuthFilter;
import com.example.switching.security.repository.ApiKeyRepository;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final ApiKeyRepository apiKeyRepository;
    private final boolean apiKeyEnabled;

    public SecurityConfig(
            ApiKeyRepository apiKeyRepository,
            @Value("${switching.security.api-key.enabled:true}") boolean apiKeyEnabled) {
        this.apiKeyRepository = apiKeyRepository;
        this.apiKeyEnabled = apiKeyEnabled;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        // Disable CSRF (stateless REST API) and sessions
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        if (!apiKeyEnabled) {
            // Test / dev profile — allow all requests without auth
            http.authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
            return http.build();
        }

        // Production — require X-API-Key on all non-public endpoints
        http
                .addFilterBefore(
                        new ApiKeyAuthFilter(apiKeyRepository),
                        UsernamePasswordAuthenticationFilter.class)
                .authorizeHttpRequests(auth -> auth

                        // ── Public (no key required) ──────────────────────────────
                        .requestMatchers(
                                "/actuator/health",
                                "/actuator/info",
                                "/swagger-ui.html",
                                "/swagger-ui/**",
                                "/v3/api-docs/**"
                        ).permitAll()

                        // ── BANK role — payment path ──────────────────────────────
                        .requestMatchers(HttpMethod.POST, "/api/inquiries").hasAnyRole("BANK", "ADMIN")
                        .requestMatchers(HttpMethod.GET,  "/api/inquiries/**").hasAnyRole("BANK", "OPS", "ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/transfers").hasAnyRole("BANK", "ADMIN")
                        .requestMatchers(HttpMethod.GET,  "/api/transfers/**").hasAnyRole("BANK", "OPS", "ADMIN")
                        .requestMatchers(HttpMethod.GET,  "/api/transfers").hasAnyRole("BANK", "OPS", "ADMIN")
                        .requestMatchers("/api/iso20022/**").hasAnyRole("BANK", "ADMIN")
                        .requestMatchers(HttpMethod.GET,  "/api/iso-messages/**").hasAnyRole("BANK", "OPS", "ADMIN")
                        .requestMatchers(HttpMethod.GET,  "/api/iso-inquiries/**").hasAnyRole("BANK", "OPS", "ADMIN")

                        // ── OPS role — operations & monitoring ────────────────────
                        .requestMatchers("/api/operations/**").hasAnyRole("OPS", "ADMIN")
                        .requestMatchers("/api/outbox-events/**").hasAnyRole("OPS", "ADMIN")

                        // ── ADMIN only — configuration management ─────────────────
                        .requestMatchers(HttpMethod.POST,  "/api/participants").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/api/participants/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET,   "/api/participants/**").hasAnyRole("OPS", "ADMIN")
                        .requestMatchers(HttpMethod.GET,   "/api/participants").hasAnyRole("OPS", "ADMIN")
                        .requestMatchers(HttpMethod.POST,  "/api/routing-rules/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/api/routing-rules/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET,   "/api/routing-rules/**").hasAnyRole("OPS", "ADMIN")
                        .requestMatchers(HttpMethod.POST,  "/api/connector-configs").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/api/connector-configs/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET,   "/api/connector-configs/**").hasAnyRole("OPS", "ADMIN")

                        // ── Catch-all — must be authenticated ─────────────────────
                        .anyRequest().authenticated()
                )
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((req, res, e) -> {
                            res.setStatus(401);
                            res.setContentType("application/json");
                            res.getWriter().write("""
                                    {"status":401,"error":"UNAUTHORIZED","errorCode":"SEC-001",\
                                    "message":"Missing or invalid X-API-Key header",\
                                    "path":"%s"}""".formatted(req.getRequestURI()));
                        })
                        .accessDeniedHandler((req, res, e) -> {
                            res.setStatus(403);
                            res.setContentType("application/json");
                            res.getWriter().write("""
                                    {"status":403,"error":"FORBIDDEN","errorCode":"SEC-002",\
                                    "message":"Insufficient role for this endpoint",\
                                    "path":"%s"}""".formatted(req.getRequestURI()));
                        })
                );

        return http.build();
    }
}
