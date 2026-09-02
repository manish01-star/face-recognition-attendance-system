package com.college.attendance.dto.course;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CourseResponse {

    private Long id;

    private String name;

    private String code;

    private Long departmentId;

    private String status;
}