package com.college.attendance.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Builder
public class LoginResponse {

    private String accessToken;

    private String tokenType;

    private String username;

    private String role;
}