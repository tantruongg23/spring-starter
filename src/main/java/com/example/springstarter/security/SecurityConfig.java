package com.example.springstarter.security;

import com.example.springstarter.domain.enumerate.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;
import java.util.List;

/**
 * Central Spring Security configuration.
 *
 * <p>JWT validation is now handled by Spring Security's built-in
 * {@code BearerTokenAuthenticationFilter} via {@code .oauth2ResourceServer()}.
 * The custom {@code JwtAuthenticationFilter} has been removed.</p>
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    private final CustomAccessDeniedHandler customAccessDeniedHandler;
    private final UserDetailsServiceImpl userDetailsService;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, JwtDecoder jwtDecoder) throws Exception {
        http
                // Disable CSRF — stateless JWT API does not need it
                .csrf(AbstractHttpConfigurer::disable)

                // Stateless session — no HttpSession created or used
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // Authorization rules
                .authorizeHttpRequests(auth -> auth
                        // Auth endpoints are public
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()

                        // Product management is ADMIN only
                        .requestMatchers(HttpMethod.POST, "/api/products/**").hasRole(Role.ADMIN.name())
                        .requestMatchers(HttpMethod.PUT, "/api/products/**").hasRole(Role.ADMIN.name())
                        .requestMatchers(HttpMethod.DELETE, "/api/products/**").hasRole(Role.ADMIN.name())

                        // User management (create) is ADMIN only
                        .requestMatchers(HttpMethod.POST, "/api/users/**").hasRole(Role.ADMIN.name())

                        // Everything else requires authentication
                        .anyRequest().authenticated()
                )

                // Custom exception handling for 401/403
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(jwtAuthenticationEntryPoint)
                        .accessDeniedHandler(customAccessDeniedHandler)
                )

                // ── OAuth2 Resource Server ────────────────────────────────────────
                // Replaces the old JwtAuthenticationFilter. Spring Security's
                // BearerTokenAuthenticationFilter extracts the Bearer token,
                // NimbusJwtDecoder verifies signature + expiry (no DB call),
                // and JwtAuthenticationConverter maps claims to GrantedAuthorities.
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt
                                .decoder(jwtDecoder)
                                .jwtAuthenticationConverter(jwtAuthenticationConverter())
                        )
                        .authenticationEntryPoint(jwtAuthenticationEntryPoint)
                )

                // Set the custom authentication provider (used during /login only)
                .authenticationProvider(authenticationProvider())
                .formLogin(AbstractHttpConfigurer::disable);

        return http.build();
    }

    /**
     * Decodes and verifies JWTs using HMAC-SHA256 with our secret key.
     * This bean is shared between the resource server filter and AuthService.
     */
    @Bean
    public JwtDecoder jwtDecoder(@Value("${security.jwt.secret-key}") String secretKey) {
        byte[] keyBytes = Base64.getDecoder().decode(secretKey);
        SecretKeySpec secretKeySpec = new SecretKeySpec(keyBytes, "HmacSHA256");
        return NimbusJwtDecoder.withSecretKey(secretKeySpec).build();
    }

    /**
     * Maps the {@code "role"} claim in the JWT to a Spring Security {@link org.springframework.security.core.GrantedAuthority}.
     * <p>
     * The access token carries {@code "role": "ROLE_ADMIN"} (or {@code "ROLE_CUSTOMER"}).
     * The converter reads that single claim and creates a {@link SimpleGrantedAuthority} from it,
     * so {@code @PreAuthorize("hasRole('ADMIN')")} and URL-pattern rules continue to work unchanged.
     * </p>
     */
    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            String role = jwt.getClaimAsString(JwtService.ROLE_CLAIM);
            if (role == null || role.isBlank()) {
                return List.of();
            }
            return List.of(new SimpleGrantedAuthority(role));
        });
        return converter;
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
