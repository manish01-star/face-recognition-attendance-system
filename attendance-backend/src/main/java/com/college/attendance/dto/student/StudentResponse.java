package com.college.attendance.dto.student;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class StudentResponse {

    private Long id;

    private Long userId;

    private String username;

    private String name;

    private String rollNumber;

    private String phone;

    private String email;

    private Long courseId;

    private Long semesterId;

    private Long sectionId;

    private String status;
}