package com.college.attendance.dto.policy;

import java.math.BigDecimal;

import com.college.attendance.entity.enums.Status;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AttendancePolicyRequest {

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

}