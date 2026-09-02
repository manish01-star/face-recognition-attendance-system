package com.college.attendance.security;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        // No Authorization header
        if (authHeader == null ||
                !authHeader.startsWith("Bearer ")) {

            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);

        try {

            // Invalid / expired token
            if (!jwtService.isTokenValid(token)) {

                filterChain.doFilter(request, response);
                return;
            }

            String username =
                    jwtService.extractUsername(token);

            Claims claims =
                    jwtService.getClaimsFromToken(token);

            String role =
                    claims.get("role", String.class);

            if (username != null &&
                    SecurityContextHolder
                            .getContext()
                            .getAuthentication() == null) {

                /*
                 * Explicitly declare the type as
                 * List<GrantedAuthority>
                 */
                List<GrantedAuthority> authorities;

                if (role != null && !role.isBlank()) {

                    authorities = Collections.singletonList(
                            new SimpleGrantedAuthority(
                                    "ROLE_" + role
                            )
                    );

                } else {

                    authorities = Collections.emptyList();
                }

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                username,
                                null,
                                authorities
                        );

                SecurityContextHolder
                        .getContext()
                        .setAuthentication(authentication);
            }

        } catch (Exception e) {

            // Do not authenticate request
            SecurityContextHolder.clearContext();

        }

        filterChain.doFilter(request, response);
    }
}