package com.college.attendance.dto.semester;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SemesterRequest {

    @NotNull(message = "Course is required")
    private Long courseId;

    @NotNull(message = "Semester number is required")
    @Min(value = 1, message = "Semester must be at least 1")
    @Max(value = 10, message = "Invalid semester")
    private Integer semesterNumber;
}