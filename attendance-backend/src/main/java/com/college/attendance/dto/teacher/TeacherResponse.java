package com.college.attendance.dto.teacher;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TeacherResponse {

    private Long id;

    private Long userId;

    private String username;

    private String name;

    private String employeeCode;

    private String phone;

    private String email;

    private Long departmentId;

    private String departmentName;

    private String status;

    private String profileImageUrl;
}