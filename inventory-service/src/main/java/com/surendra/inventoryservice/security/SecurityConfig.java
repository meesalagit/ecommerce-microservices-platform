package com.surendra.inventoryservice.security;

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
    private final InternalServiceKeyFilter internalServiceKeyFilter;

    public SecurityConfig(
            JwtAuthenticationFilter jwtAuthenticationFilter,
            InternalServiceKeyFilter internalServiceKeyFilter
    ) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.internalServiceKeyFilter = internalServiceKeyFilter;
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

                        // Internal Order Service call:
                        // internal key is checked by InternalServiceKeyFilter
                        .requestMatchers(
                                HttpMethod.GET,
                                "/inventory/*/available"
                        ).permitAll()

                        // Internal Order Service call:
                        // internal key is checked by InternalServiceKeyFilter
                        .requestMatchers(
                                HttpMethod.PUT,
                                "/inventory/*/reduce"
                        ).permitAll()

                        .requestMatchers(
                                HttpMethod.PUT,
                                "/inventory/*/restore"
                        ).permitAll()

                        // Only ADMIN can create inventory
                        .requestMatchers(
                                HttpMethod.POST,
                                "/inventory"
                        ).hasRole("ADMIN")

                        // Only ADMIN can add stock
                        .requestMatchers(
                                HttpMethod.PUT,
                                "/inventory/*"
                        ).hasRole("ADMIN")

                        // Authenticated users can get inventory details
                        .requestMatchers(
                                HttpMethod.GET,
                                "/inventory/*"
                        ).authenticated()

                        .anyRequest().authenticated()
                )

                // Check internal service key first
                .addFilterBefore(
                        internalServiceKeyFilter,
                        UsernamePasswordAuthenticationFilter.class
                )

                .addFilterBefore(
                        jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class
                );

        return http.build();
    }
}