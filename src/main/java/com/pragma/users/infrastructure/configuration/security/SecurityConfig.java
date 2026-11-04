package com.pragma.users.infrastructure.configuration.security;

import com.pragma.users.infrastructure.out.security.JwtTokenProviderAdapter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private static final String ADMINISTRATOR_ROLE = "ADMINISTRATOR";
    private static final String OWNER_ROLE = "OWNER";
    private static final String OWNER_ENDPOINT = "/api/v1/users/owner";
    private static final String EMPLOYEE_ENDPOINT = "/api/v1/users/employee";
    private static final String[] PUBLIC_ENDPOINTS = {
            "/api/v1/auth/**",
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/v3/api-docs/**"
    };

    // CSRF is disabled on purpose: this is a stateless REST API authenticated with a JWT Bearer
    // token (no cookies, no server-side session), so CSRF — which exploits ambient cookie
    // credentials the browser sends automatically — does not apply here.
    @SuppressWarnings("java:S4502")
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   JwtTokenProviderAdapter jwtTokenProviderAdapter)
            throws Exception {
        JwtAuthenticationFilter jwtAuthenticationFilter =
                new JwtAuthenticationFilter(jwtTokenProviderAdapter);

        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(exception ->
                        exception.authenticationEntryPoint(new JwtAuthenticationEntryPoint()))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(PUBLIC_ENDPOINTS).permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/users/**").permitAll()
                        .requestMatchers(HttpMethod.POST, OWNER_ENDPOINT).hasRole(ADMINISTRATOR_ROLE)
                        .requestMatchers(HttpMethod.POST, EMPLOYEE_ENDPOINT).hasRole(OWNER_ROLE)
                        .anyRequest().authenticated())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
