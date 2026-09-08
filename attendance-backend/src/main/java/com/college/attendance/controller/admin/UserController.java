package com.college.attendance.controller.admin;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.college.attendance.dto.user.UserProfileResponse;
import com.college.attendance.service.UserService;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;

@RestController
@RequestMapping("/api/users")
@SecurityRequirement(name = "bearerAuth")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    // =========================================================
    // USER - GET MY PROFILE
    // =========================================================

    @GetMapping("/profile")
    public ResponseEntity<UserProfileResponse> getMyProfile(
            @RequestAttribute("userId") Long userId) {

        return ResponseEntity.ok(
                userService.getMyProfile(userId));
    }
}