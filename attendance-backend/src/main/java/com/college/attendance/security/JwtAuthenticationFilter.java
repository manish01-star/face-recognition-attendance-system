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
public class JwtAuthenticationFilter
        extends OncePerRequestFilter {

    private final JwtService jwtService;


    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

        String authHeader =
                request.getHeader("Authorization");


        /*
         * ============================================================
         * NO TOKEN
         * ============================================================
         */

        if (authHeader == null ||
                !authHeader.startsWith("Bearer ")) {

            filterChain.doFilter(request, response);
            return;
        }


        String token =
                authHeader.substring(7);


        try {

            /*
             * ========================================================
             * VALIDATE TOKEN
             * ========================================================
             */

            if (!jwtService.isTokenValid(token)) {

                filterChain.doFilter(request, response);
                return;
            }


            /*
             * ========================================================
             * EXTRACT USERNAME
             * ========================================================
             */

            String username =
                    jwtService.extractUsername(token);


            /*
             * ========================================================
             * EXTRACT CLAIMS
             * ========================================================
             */

            Claims claims =
                    jwtService.getClaimsFromToken(token);


            /*
             * ========================================================
             * EXTRACT ROLE
             * ========================================================
             */

            String role =
                    claims.get("role", String.class);


            /*
             * ========================================================
             * CREATE AUTHENTICATION
             * ========================================================
             */

            if (username != null &&
                    SecurityContextHolder
                            .getContext()
                            .getAuthentication() == null) {

                List<GrantedAuthority> authorities =
                        Collections.emptyList();


                if (role != null &&
                        !role.isBlank()) {

                    authorities = Collections.singletonList(
                            new SimpleGrantedAuthority(
                                    "ROLE_" + role
                            )
                    );
                }


                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                username,
                                null,
                                authorities
                        );


                SecurityContextHolder
                        .getContext()
                        .setAuthentication(
                                authentication
                        );
            }

        } catch (Exception e) {

            /*
             * Invalid JWT
             */
            SecurityContextHolder.clearContext();
        }


        filterChain.doFilter(request, response);
    }
}