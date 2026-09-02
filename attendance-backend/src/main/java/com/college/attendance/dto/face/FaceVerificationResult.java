package com.college.attendance.dto.face;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class FaceVerificationResult {

    private Long userId;

    private boolean verified;

    private double distance;

    private double threshold;

    private String model;

    private String message;
}