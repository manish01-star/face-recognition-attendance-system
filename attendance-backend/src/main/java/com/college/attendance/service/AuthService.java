package com.college.attendance.service;

import com.college.attendance.dto.auth.LoginRequest;
import com.college.attendance.dto.auth.LoginResponse;
import com.college.attendance.entity.User;
import com.college.attendance.entity.enums.Role;
import com.college.attendance.entity.enums.UserStatus;
import com.college.attendance.repository.UserRepository;
import com.college.attendance.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;

    private final PasswordEncoder passwordEncoder;

    private final JwtService jwtService;


    /**
     * Admin Login
     *
     * Login is allowed only when:
     *
     * 1. Username exists
     * 2. User status is ACTIVE
     * 3. Password is correct
     * 4. User role is ADMIN
     */
    public LoginResponse login(LoginRequest request) {

        /*
         * ============================================================
         * FIND USER
         * ============================================================
         */

        User user = userRepository
                .findByUsername(request.getUsername())
                .orElseThrow(() ->
                        new RuntimeException(
                                "Invalid username or password"
                        )
                );


        /*
         * ============================================================
         * CHECK USER STATUS
         * ============================================================
         */

        if (user.getStatus() != UserStatus.ACTIVE) {

            throw new RuntimeException(
                    "User account is inactive"
            );
        }


        /*
         * ============================================================
         * CHECK PASSWORD
         * ============================================================
         */

        if (!passwordEncoder.matches(
                request.getPassword(),
                user.getPassword())) {

            throw new RuntimeException(
                    "Invalid username or password"
            );
        }


        /*
         * ============================================================
         * CHECK ADMIN ROLE
         * ============================================================
         */

        if (user.getRole() != Role.ADMIN) {

            throw new RuntimeException(
                    "Access denied. Admin login required."
            );
        }


        /*
         * ============================================================
         * GENERATE JWT
         * ============================================================
         */

        String token =
                jwtService.generateToken(user);


        /*
         * ============================================================
         * LOGIN RESPONSE
         * ============================================================
         *
         * LoginResponse currently contains:
         *
         * accessToken
         * tokenType
         * username
         * role
         *
         */

        return LoginResponse.builder()

                .accessToken(token)

                .tokenType("Bearer")

                .username(user.getUsername())

                .role(user.getRole().name())

                .build();
    }
}
