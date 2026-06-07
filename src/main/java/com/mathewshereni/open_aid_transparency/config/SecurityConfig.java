package com.mathewshereni.open_aid_transparency.config;

import com.mathewshereni.open_aid_transparency.security.JwtAuthenticationFilter;
import com.mathewshereni.open_aid_transparency.security.JwtService;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Real JWT security configuration.
 *
 * Public endpoints need no token; reads need any valid login; writes to core
 * data need ADMIN. Our JwtAuthenticationFilter runs first to identify the user
 * from their bearer token, then these rules decide allow/deny.
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtService jwtService;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // Stateless token-based API: no CSRF tokens, no server-side sessions.
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Don't re-authorize internal ERROR/ASYNC dispatches, otherwise a
                        // 403 can be overwritten by a 401 when the error page is rendered.
                        .dispatcherTypeMatchers(DispatcherType.ERROR, DispatcherType.ASYNC).permitAll()

                        // ---- PUBLIC: no token required ----
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/actuator/health").permitAll()
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/funding-flows/reports/**").permitAll()

                        // ---- ADMIN only: user administration (incl. READS, which expose emails/roles) ----
                        .requestMatchers(HttpMethod.GET, "/api/users/**").hasRole("ADMIN")

                        // ---- ADMIN only: creating / changing core data ----
                        .requestMatchers(HttpMethod.POST, "/api/donors/**", "/api/recipients/**",
                                "/api/aid-projects/**", "/api/funding-flows/**", "/api/users/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/donors/**", "/api/recipients/**",
                                "/api/aid-projects/**", "/api/funding-flows/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/donors/**", "/api/recipients/**",
                                "/api/aid-projects/**", "/api/funding-flows/**", "/api/users/**",
                                "/api/feedback/**").hasRole("ADMIN")

                        // ---- ADMIN only: data import jobs ----
                        .requestMatchers("/api/ingestion/**").hasRole("ADMIN")

                        // ---- any logged-in user can submit feedback ----
                        .requestMatchers(HttpMethod.POST, "/api/feedback/**").authenticated()

                        // ---- everything else (the GET reads) requires a valid login ----
                        .anyRequest().authenticated()
                )
                // Missing/invalid token on a protected route -> 401 (not the default 403).
                .exceptionHandling(ex -> ex.authenticationEntryPoint(
                        (request, response, authEx) ->
                                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Authentication required")))
                // Identify the user from their JWT before the username/password filter runs.
                .addFilterBefore(new JwtAuthenticationFilter(jwtService), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * BCrypt password hasher. We use it to hash a user's password before saving,
     * and (in Phase 3) to verify a login password against the stored hash.
     * Exposing it as a @Bean lets Spring inject it into any service that needs it.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Exposes Spring's AuthenticationManager as a bean so our login endpoint can
     * inject it. Behind the scenes it uses our AppUserDetailsService (to load the
     * user) and the PasswordEncoder above (to check the password).
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }
}
