package com.college.attendance.config;

import com.college.attendance.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

        private final JwtAuthenticationFilter jwtAuthenticationFilter;

        @Bean
        public PasswordEncoder passwordEncoder() {
                return new BCryptPasswordEncoder();
        }

        @Bean
        public SecurityFilterChain securityFilterChain(
                        HttpSecurity http) throws Exception {

                http

                                .csrf(csrf -> csrf.disable())

                                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                                .sessionManagement(session -> session.sessionCreationPolicy(
                                                SessionCreationPolicy.STATELESS))

                                .authorizeHttpRequests(auth -> auth

                                                // =========================
                                                // PUBLIC STATIC PAGES
                                                // =========================

                                                .requestMatchers(
                                                                "/",
                                                                "/login.html",
                                                                "/dashboard.html",
                                                                "/students.html",
                                                                "/teachers.html",
                                                                "/departments.html",
                                                                "/courses.html",
                                                                "/semesters.html",
                                                                "/sections.html",
                                                                "/attendance.html",
                                                                "/machine-attendance.html")
                                                .permitAll()

                                                // =========================
                                                // STATIC RESOURCES
                                                // =========================

                                                .requestMatchers(
                                                                "/css/**",
                                                                "/auth.js",
                                                                "/js/**",
                                                                "/images/**",
                                                                "/favicon.ico")
                                                .permitAll()

                                                // =========================
                                                // SWAGGER
                                                // =========================

                                                .requestMatchers(
                                                                "/swagger-ui/**",
                                                                "/swagger-ui.html",
                                                                "/v3/api-docs/**")
                                                .permitAll()

                                                // =========================
                                                // LOGIN
                                                // =========================

                                                .requestMatchers(
                                                                "/api/auth/login")
                                                .permitAll()

                                                // =========================
                                                // MACHINE ATTENDANCE
                                                // =========================

                                                .requestMatchers(
                                                                "/api/attendance/machine/**")
                                                .permitAll()

                                                // =========================
                                                // ADMIN APIs
                                                // =========================

                                                .requestMatchers(
                                                                "/api/admin/**")
                                                .hasRole("ADMIN")

                                                // =========================
                                                // EVERYTHING ELSE
                                                // =========================

                                                .anyRequest()
                                                .authenticated())

                                .addFilterBefore(
                                                jwtAuthenticationFilter,
                                                UsernamePasswordAuthenticationFilter.class);

                return http.build();
        }

        @Bean
        public CorsConfigurationSource corsConfigurationSource() {

                CorsConfiguration configuration = new CorsConfiguration();

                configuration.setAllowedOrigins(
                                List.of(
                                                "http://localhost:8080",
                                                "http://127.0.0.1:8080",
                                                "null"));

                configuration.setAllowedMethods(
                                List.of(
                                                "GET",
                                                "POST",
                                                "PUT",
                                                "DELETE",
                                                "PATCH",
                                                "OPTIONS"));

                configuration.setAllowedHeaders(
                                List.of(
                                                "*"));

                configuration.setExposedHeaders(
                                List.of(
                                                "Authorization"));

                configuration.setAllowCredentials(false);

                UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();

                source.registerCorsConfiguration(
                                "/**",
                                configuration);

                return source;
        }
}