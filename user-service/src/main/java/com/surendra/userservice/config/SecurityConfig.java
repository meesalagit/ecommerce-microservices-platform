package com.surendra.userservice.config;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {

    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http
    ) throws Exception {

        http
                .csrf(csrf -> csrf.disable())

                .sessionManagement(session -> session
                        .sessionCreationPolicy(
                                SessionCreationPolicy.STATELESS
                        )
                )

                .exceptionHandling(exception -> exception

                        .authenticationEntryPoint(
                                (request, response, authException) ->
                                        response.sendError(
                                                HttpServletResponse.SC_UNAUTHORIZED,
                                                "Unauthorized"
                                        )
                        )

                        .accessDeniedHandler(
                                (request, response, accessDeniedException) ->
                                        response.setStatus(
                                                HttpServletResponse.SC_FORBIDDEN
                                        )
                        )
                )

                .authorizeHttpRequests(auth -> auth

                        // Registration is public
                        .requestMatchers(
                                HttpMethod.POST,
                                "/users"
                        ).permitAll()

                        // Login is public
                        .requestMatchers(
                                HttpMethod.POST,
                                "/users/login"
                        ).permitAll()

                        // Admin endpoints still require ADMIN role
                        .requestMatchers(
                                "/admin/**"
                        ).hasRole("ADMIN")

                        // Everything else requires authentication
                        .anyRequest().authenticated()
                )

                .addFilterBefore(
                        jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class
                );

        return http.build();
    }
}