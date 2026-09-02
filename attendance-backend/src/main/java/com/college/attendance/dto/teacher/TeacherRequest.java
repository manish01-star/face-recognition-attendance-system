package com.college.attendance.dto.teacher;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TeacherRequest {

    @NotBlank(message = "Teacher name is required")
    private String name;

    @NotBlank(message = "Employee code is required")
    private String employeeCode;

    @NotBlank(message = "Phone number is required")
    @Pattern(
            regexp = "^[6-9][0-9]{9}$",
            message = "Invalid Indian phone number"
    )
    private String phone;

    @Email(message = "Invalid email")
    private String email;

    @NotNull(message = "Department is required")
    private Long departmentId;
}