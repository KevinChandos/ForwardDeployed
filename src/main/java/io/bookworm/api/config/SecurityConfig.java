package io.bookworm.api.config;

import io.bookworm.api.security.CustomAccessDeniedHandler;
import io.bookworm.api.security.JwtAuthenticationEntryPoint;
import io.bookworm.api.security.JwtFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * Spring Security configuration defining filter chain, CORS, password hashing, and authorization rules.
 * <p>
 * Why: Configures stateless JWT authentication, secures HTTP endpoints with role-based access control
 * (RBAC: GUEST, CUSTOMER/REGISTERED_USER, ADMIN/STORE_ADMIN/CATALOGUE_MANAGER/PLATFORM_ADMIN),
 * and activates method-level security with {@code @EnableMethodSecurity}.
 * <p>
 * Side effects: Configures global security filter pipeline, exception handlers, and security headers.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtFilter jwtFilter;
    private final JwtAuthenticationEntryPoint authenticationEntryPoint;
    private final CustomAccessDeniedHandler accessDeniedHandler;

    @Value("${bookworm.security.cors.allowed-origins:http://localhost:3000,http://localhost:8080}")
    private String allowedOrigins;

    /**
     * Password encoder bean using BCrypt hashing with work factor 12.
     * Required by AuthenticationManager and identity services.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    /**
     * Exposes AuthenticationManager bean from Spring Security configuration.
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    /**
     * Configures the main {@link SecurityFilterChain} and authorization rules.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(Customizer.withDefaults())
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler)
                )
                .authorizeHttpRequests(auth -> auth
                        // ── Public Auth Endpoints ───────────────────────────────────────────────
                        .requestMatchers(HttpMethod.POST,
                                "/auth/register",
                                "/auth/login",
                                "/auth/refresh",
                                "/auth/password/forgot",
                                "/auth/password/reset"
                        ).permitAll()

                        // ── Public API Docs & Actuator ──────────────────────────────────────────
                        .requestMatchers(
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/actuator/health",
                                "/actuator/info"
                        ).permitAll()

                        // ── Public Catalogue Browsing (GUEST allowed) ───────────────────────────
                        .requestMatchers(HttpMethod.GET,
                                "/books/**",
                                "/authors/**",
                                "/publishers/**",
                                "/categories/**",
                                "/search/**",
                                "/recommendations/**",
                                "/reviews/books/**"
                        ).permitAll()

                        // ── Webhook / Payment Callbacks ─────────────────────────────────────────
                        .requestMatchers(HttpMethod.POST, "/payments/webhook").permitAll()

                        // ── Admin-Only Endpoints (ADMIN, STORE_ADMIN, CATALOGUE_MANAGER, PLATFORM_ADMIN) ──
                        .requestMatchers(HttpMethod.POST, "/books/**", "/authors/**", "/publishers/**", "/categories/**")
                        .hasAnyRole("ADMIN", "CATALOGUE_MANAGER", "PLATFORM_ADMIN", "STORE_ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/books/**", "/authors/**", "/publishers/**", "/categories/**")
                        .hasAnyRole("ADMIN", "CATALOGUE_MANAGER", "PLATFORM_ADMIN", "STORE_ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/books/**", "/authors/**", "/publishers/**", "/categories/**")
                        .hasAnyRole("ADMIN", "CATALOGUE_MANAGER", "PLATFORM_ADMIN", "STORE_ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/books/**", "/authors/**", "/publishers/**", "/categories/**")
                        .hasAnyRole("ADMIN", "CATALOGUE_MANAGER", "PLATFORM_ADMIN", "STORE_ADMIN")

                        .requestMatchers("/reviews/moderation/**", "/reviews/*/publish", "/reviews/*/reject")
                        .hasAnyRole("ADMIN", "STORE_ADMIN", "PLATFORM_ADMIN")

                        .requestMatchers(HttpMethod.POST, "/coupons/**").hasAnyRole("ADMIN", "STORE_ADMIN", "PLATFORM_ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/coupons/**").hasAnyRole("ADMIN", "STORE_ADMIN", "PLATFORM_ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/coupons/**").hasAnyRole("ADMIN", "STORE_ADMIN", "PLATFORM_ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/coupons/**").hasAnyRole("ADMIN", "STORE_ADMIN", "PLATFORM_ADMIN")

                        // ── Customer / Authenticated Member Endpoints ───────────────────────────
                        .requestMatchers("/auth/logout").authenticated()
                        .requestMatchers("/users/me/**").hasAnyRole("CUSTOMER", "REGISTERED_USER", "ADMIN", "STORE_ADMIN", "PLATFORM_ADMIN")
                        .requestMatchers("/checkout/**", "/orders/**").hasAnyRole("CUSTOMER", "REGISTERED_USER", "ADMIN", "STORE_ADMIN", "PLATFORM_ADMIN")
                        .requestMatchers("/payments/**").hasAnyRole("CUSTOMER", "REGISTERED_USER", "ADMIN", "STORE_ADMIN", "PLATFORM_ADMIN")
                        .requestMatchers(HttpMethod.POST, "/reviews").hasAnyRole("CUSTOMER", "REGISTERED_USER", "ADMIN", "STORE_ADMIN", "PLATFORM_ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/reviews/**").hasAnyRole("CUSTOMER", "REGISTERED_USER", "ADMIN", "STORE_ADMIN", "PLATFORM_ADMIN")

                        // ── Cart (Guests & Customers) ───────────────────────────────────────────
                        .requestMatchers("/cart/**").permitAll()

                        // ── All other requests require authentication ───────────────────────────
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * CORS configuration allowing configured origins, standard HTTP methods, and auth headers.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(Arrays.asList(allowedOrigins.split(",")));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS", "HEAD"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-Correlation-ID", "X-Guest-Token", "X-Idempotency-Key"));
        config.setExposedHeaders(List.of("X-Correlation-ID", "Authorization"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
