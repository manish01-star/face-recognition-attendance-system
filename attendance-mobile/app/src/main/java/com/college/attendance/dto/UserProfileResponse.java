package com.college.attendance.dto;

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

    public Long getUserId() {
        return userId;
    }

    public String getUsername() {
        return username;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getPhone() {
        return phone;
    }

    public String getRole() {
        return role;
    }

    public String getRollNumber() {
        return rollNumber;
    }

    public String getEmployeeNumber() {
        return employeeNumber;
    }

    public String getProfileImageUrl() {
        return profileImageUrl;
    }
}