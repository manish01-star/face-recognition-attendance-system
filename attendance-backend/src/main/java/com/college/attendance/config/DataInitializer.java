package com.college.attendance.config;

import com.college.attendance.entity.User;
import com.college.attendance.entity.enums.Role;
import com.college.attendance.entity.enums.UserStatus;
import com.college.attendance.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {

        if (userRepository.existsByRole(Role.ADMIN)) {
            return;
        }

        User admin = User.builder()
                .username("admin")
                .password(passwordEncoder.encode("admin123"))
                .role(Role.ADMIN)
                .status(UserStatus.ACTIVE)
                .build();

        userRepository.save(admin);

        System.out.println("=================================");
        System.out.println("Default ADMIN created");
        System.out.println("Username : admin");
        System.out.println("Password : 12345678");
        System.out.println("=================================");
    }
}