package com.college.attendance.dto.student;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class StudentRequest {

    @NotBlank(message = "Student name is required")
    private String name;

    @NotBlank(message = "Roll number is required")
    private String rollNumber;

    @NotBlank(message = "Phone number is required")
    @Pattern(
            regexp = "^[6-9][0-9]{9}$",
            message = "Invalid Indian phone number"
    )
    private String phone;

    @Email(message = "Invalid email")
    private String email;

    @NotNull(message = "Course is required")
    private Long courseId;

    @NotNull(message = "Semester is required")
    private Long semesterId;

    @NotNull(message = "Section is required")
    private Long sectionId;
}