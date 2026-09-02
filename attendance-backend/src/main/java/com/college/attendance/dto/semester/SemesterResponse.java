package com.college.attendance.dto.semester;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SemesterResponse {

    private Long id;

    private Long courseId;

    private Integer semesterNumber;

    private String status;
}