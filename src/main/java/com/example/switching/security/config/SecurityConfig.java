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
import com.example.switching.security.mtls.MtlsCertificateValidator;
import com.example.switching.security.mtls.MtlsFilter;
import com.example.switching.security.oauth.OAuthTokenFilter;
import com.example.switching.security.oauth.service.OAuthTokenService;
import com.example.switching.security.repository.ApiKeyRepository;
import com.example.switching.security.signing.HmacSignatureVerifier;
import com.example.switching.security.signing.RequestSignatureFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final ApiKeyRepository apiKeyRepository;
    private final HmacSignatureVerifier hmacSignatureVerifier;
    private final OAuthTokenService oAuthTokenService;
    private final MtlsCertificateValidator mtlsCertificateValidator;
    private final boolean apiKeyEnabled;
    private final boolean oauthEnabled;
    private final boolean mtlsEnabled;
    private final String mtlsCertHeader;
    private final boolean signingEnabled;

    public SecurityConfig(
            ApiKeyRepository apiKeyRepository,
            HmacSignatureVerifier hmacSignatureVerifier,
            OAuthTokenService oAuthTokenService,
            MtlsCertificateValidator mtlsCertificateValidator,
            @Value("${switching.security.api-key.enabled:true}") boolean apiKeyEnabled,
            @Value("${switching.security.oauth.enabled:false}") boolean oauthEnabled,
            @Value("${switching.security.mtls.enabled:false}") boolean mtlsEnabled,
            @Value("${switching.security.mtls.cert-header:X-Client-Cert}") String mtlsCertHeader,
            @Value("${switching.security.signing.enabled:false}") boolean signingEnabled) {
        this.apiKeyRepository = apiKeyRepository;
        this.hmacSignatureVerifier = hmacSignatureVerifier;
        this.oAuthTokenService = oAuthTokenService;
        this.mtlsCertificateValidator = mtlsCertificateValidator;
        this.apiKeyEnabled = apiKeyEnabled;
        this.oauthEnabled = oauthEnabled;
        this.mtlsEnabled = mtlsEnabled;
        this.mtlsCertHeader = mtlsCertHeader;
        this.signingEnabled = signingEnabled;
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

        // ── Auth filter chain ─────────────────────────────────────────────────
        //
        // Order (when both flags are on):
        //   OAuthTokenFilter → ApiKeyAuthFilter → RequestSignatureFilter
        //
        // OAuthTokenFilter activates only when Authorization: Bearer is present.
        // ApiKeyAuthFilter activates only when X-API-Key is present.
        // Both can coexist during the grace period while PSPs migrate to OAuth.

        ApiKeyAuthFilter apiKeyAuthFilter = new ApiKeyAuthFilter(apiKeyRepository);
        http.addFilterBefore(apiKeyAuthFilter, UsernamePasswordAuthenticationFilter.class);

        if (oauthEnabled) {
            // Run OAuth before ApiKey so Bearer tokens are authenticated first.
            http.addFilterBefore(new OAuthTokenFilter(oAuthTokenService), ApiKeyAuthFilter.class);
        }

        if (mtlsEnabled) {
            // mTLS runs after OAuthTokenFilter (cert validates the channel, OAuth validates the caller).
            // Skips public paths (/v1/oauth/token, /actuator/health, etc.) — see MtlsFilter.shouldNotFilter.
            http.addFilterAfter(new MtlsFilter(mtlsCertificateValidator, mtlsCertHeader), ApiKeyAuthFilter.class);
        }

        if (signingEnabled) {
            http.addFilterAfter(new RequestSignatureFilter(hmacSignatureVerifier), ApiKeyAuthFilter.class);
        }

        http.authorizeHttpRequests(auth -> auth

                        // ── Public (no key required) ──────────────────────────────
                        .requestMatchers(
                                "/actuator/health",
                                "/actuator/info",
                                "/swagger-ui.html",
                                "/swagger-ui/**",
                                "/v3/api-docs/**",
                                // OAuth token endpoint — PSPs authenticate *here* with
                                // client_id + client_secret; Bearer token is returned
                                "/v1/oauth/token",
                                "/v1/oauth/token/revoke"
                        ).permitAll()

                        // ── Ops/Internal actuator endpoints — never expose publicly ──
                        // /actuator/prometheus is served on management port 9090 in prod
                        // (see MANAGEMENT_PORT in configmap.yaml) so this rule is a
                        // defence-in-depth guard if both ports are accidentally proxied.
                        .requestMatchers(
                                "/actuator/prometheus",
                                "/actuator/metrics",
                                "/actuator/metrics/**",
                                "/actuator/env",
                                "/actuator/env/**",
                                "/actuator/beans",
                                "/actuator/configprops"
                        ).hasAnyRole("OPS", "ADMIN")

                        // ── BANK role — payment path ──────────────────────────────
                        .requestMatchers(HttpMethod.POST, "/api/inquiries").hasAnyRole("BANK", "ADMIN")
                        .requestMatchers(HttpMethod.GET,  "/api/inquiries/**").hasAnyRole("BANK", "OPS", "ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/transfers").hasAnyRole("BANK", "ADMIN")
                        .requestMatchers(HttpMethod.GET,  "/api/transfers/**").hasAnyRole("BANK", "OPS", "ADMIN")
                        .requestMatchers(HttpMethod.GET,  "/api/transfers").hasAnyRole("BANK", "OPS", "ADMIN")
                        .requestMatchers("/api/iso20022/**").hasAnyRole("BANK", "ADMIN")
                        .requestMatchers(HttpMethod.GET,  "/api/iso-messages/**").hasAnyRole("BANK", "OPS", "ADMIN")
                        .requestMatchers(HttpMethod.GET,  "/api/iso-inquiries/**").hasAnyRole("BANK", "OPS", "ADMIN")

                        // ── ADMIN only — API key management ──────────────────────
                        .requestMatchers("/api/admin/api-keys/**").hasRole("ADMIN")

                        // ── ADMIN only — destructive operations actions ──────────
                        .requestMatchers(HttpMethod.POST, "/api/operations/outbox-failures/retry-all").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/operations/outbox-stuck/recover-all").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/operations/bank-onboarding").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/operations/bank-onboarding/generate-routes").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/operations/connectors/*/test").hasRole("ADMIN")

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
