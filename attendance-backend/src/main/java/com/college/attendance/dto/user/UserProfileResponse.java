package com.college.attendance.dto.user;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserProfileResponse {

    private Long userId;

    private String username;

    private String name;

    private String email;

    private String phone;

    private String role;

    private String rollNumber;

    private String employeeNumber;

    private String profileImageUrl;
}