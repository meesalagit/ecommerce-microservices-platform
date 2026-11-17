package com.surendra.productservice.security;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(
            JwtAuthenticationFilter jwtAuthenticationFilter
    ) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
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
                                (request, response, authException) -> {
                                    response.setStatus(
                                            HttpServletResponse.SC_UNAUTHORIZED
                                    );
                                    response.setContentType("application/json");
                                    response.getWriter().write(
                                            "{\"message\":\"Authentication is required\"}"
                                    );
                                }
                        )

                        .accessDeniedHandler(
                                (request, response, accessDeniedException) -> {
                                    response.setStatus(
                                            HttpServletResponse.SC_FORBIDDEN
                                    );
                                    response.setContentType("application/json");
                                    response.getWriter().write(
                                            "{\"message\":\"Admin access is required\"}"
                                    );
                                }
                        )
                )

                .authorizeHttpRequests(auth -> auth

                        // Anyone can view products
                        .requestMatchers(
                                HttpMethod.GET,
                                "/products/**"
                        ).permitAll()

                        // Only ADMIN can create products
                        .requestMatchers(
                                HttpMethod.POST,
                                "/products/**"
                        ).hasRole("ADMIN")

                        // Only ADMIN can update products
                        .requestMatchers(
                                HttpMethod.PUT,
                                "/products/**"
                        ).hasRole("ADMIN")

                        // Only ADMIN can delete products
                        .requestMatchers(
                                HttpMethod.DELETE,
                                "/products/**"
                        ).hasRole("ADMIN")

                        .anyRequest().authenticated()
                )

                .addFilterBefore(
                        jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class
                );

        return http.build();
    }
}