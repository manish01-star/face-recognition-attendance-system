package com.college.attendance.dto.face;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class FaceRegistrationResponse {

    private Long userId;

    private String name;

    private String role;

    private String modelName;

    private String status;

    private String message;
}