package com.surendra.inventoryservice.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class InternalServiceKeyFilter extends OncePerRequestFilter {

    @Value("${internal.service.key}")
    private String internalServiceKey;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String path = request.getRequestURI();
        String method = request.getMethod();

        boolean internalInventoryEndpoint =
                path.matches("/inventory/\\d+/available")
                        || (
                        method.equals("PUT")
                                && path.matches("/inventory/\\d+/reduce")
                )
                        || (
                        method.equals("PUT")
                                && path.matches("/inventory/\\d+/restore")
                );

        if (!internalInventoryEndpoint) {
            filterChain.doFilter(request, response);
            return;
        }

        String providedKey =
                request.getHeader("X-Internal-Service-Key");

        if (providedKey == null ||
                !providedKey.equals(internalServiceKey)) {

            response.setStatus(
                    HttpServletResponse.SC_UNAUTHORIZED
            );

            response.setContentType("application/json");

            response.getWriter().write(
                    "{\"message\":\"Invalid internal service key\"}"
            );

            return;
        }

        filterChain.doFilter(request, response);
    }
}