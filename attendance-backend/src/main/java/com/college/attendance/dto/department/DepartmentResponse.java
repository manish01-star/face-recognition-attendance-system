package com.college.attendance.dto.department;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class DepartmentResponse {

    private Long id;

    private String name;

    private String code;

    private String status;
}