package com.college.attendance.dto.policy;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.college.attendance.entity.enums.Status;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AttendancePolicyResponse {

    private Long id;

    private String policyName;

    private Boolean saturdayOff;

    private Boolean sundayOff;

    private BigDecimal workingHours;

    private Boolean attendanceRequired;

    private String locationName;

    private BigDecimal latitude;

    private BigDecimal longitude;

    private BigDecimal allowedRadiusMeters;

    private Status status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

}