package com.college.attendance.dto.section;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SectionRequest {

    @NotNull(message = "Semester is required")
    private Long semesterId;

    @NotBlank(message = "Section name is required")
    private String name;
}