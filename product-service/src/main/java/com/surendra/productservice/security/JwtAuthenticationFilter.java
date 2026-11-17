package com.surendra.productservice.security;

import com.surendra.productservice.dto.UserResponseDTO;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final RestClient restClient;

    @Value("${services.user-url}")
    private String userServiceUrl;

    public JwtAuthenticationFilter(
            JwtService jwtService,
            RestClient restClient
    ) {
        this.jwtService = jwtService;
        this.restClient = restClient;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String authorizationHeader =
                request.getHeader("Authorization");

        if (authorizationHeader == null ||
                !authorizationHeader.startsWith("Bearer ")) {

            filterChain.doFilter(request, response);
            return;
        }

        String token = authorizationHeader.substring(7);

        try {

            String email = jwtService.extractEmail(token);

            if (email != null &&
                    jwtService.validateToken(token) &&
                    SecurityContextHolder.getContext()
                            .getAuthentication() == null) {

                UserResponseDTO userResponseDTO =
                        restClient.get()
                                .uri(userServiceUrl + "/users/me")
                                .header(
                                        "Authorization",
                                        authorizationHeader
                                )
                                .retrieve()
                                .body(UserResponseDTO.class);

                String role = userResponseDTO.getRole();

                SimpleGrantedAuthority authority =
                        new SimpleGrantedAuthority(
                                "ROLE_" + role
                        );

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                email,
                                null,
                                List.of(authority)
                        );

                SecurityContextHolder.getContext()
                        .setAuthentication(authentication);
            }

        } catch (Exception exception) {

            response.setStatus(
                    HttpServletResponse.SC_UNAUTHORIZED
            );

            response.setContentType("application/json");

            response.getWriter().write(
                    "{\"message\":\"Invalid or expired JWT token\"}"
            );

            return;
        }

        filterChain.doFilter(request, response);
    }
}