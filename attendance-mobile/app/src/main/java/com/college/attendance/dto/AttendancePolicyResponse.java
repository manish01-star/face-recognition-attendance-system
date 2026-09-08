package com.college.attendance.dto;

import com.google.gson.annotations.SerializedName;

import java.math.BigDecimal;

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

    private String status;

    private String createdAt;

    private String updatedAt;

    public AttendancePolicyResponse() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getPolicyName() {
        return policyName;
    }

    public void setPolicyName(String policyName) {
        this.policyName = policyName;
    }

    public Boolean getSaturdayOff() {
        return saturdayOff;
    }

    public void setSaturdayOff(Boolean saturdayOff) {
        this.saturdayOff = saturdayOff;
    }

    public Boolean getSundayOff() {
        return sundayOff;
    }

    public void setSundayOff(Boolean sundayOff) {
        this.sundayOff = sundayOff;
    }

    public BigDecimal getWorkingHours() {
        return workingHours;
    }

    public void setWorkingHours(BigDecimal workingHours) {
        this.workingHours = workingHours;
    }

    public Boolean getAttendanceRequired() {
        return attendanceRequired;
    }

    public void setAttendanceRequired(Boolean attendanceRequired) {
        this.attendanceRequired = attendanceRequired;
    }

    public String getLocationName() {
        return locationName;
    }

    public void setLocationName(String locationName) {
        this.locationName = locationName;
    }

    public BigDecimal getLatitude() {
        return latitude;
    }

    public void setLatitude(BigDecimal latitude) {
        this.latitude = latitude;
    }

    public BigDecimal getLongitude() {
        return longitude;
    }

    public void setLongitude(BigDecimal longitude) {
        this.longitude = longitude;
    }

    public BigDecimal getAllowedRadiusMeters() {
        return allowedRadiusMeters;
    }

    public void setAllowedRadiusMeters(BigDecimal allowedRadiusMeters) {
        this.allowedRadiusMeters = allowedRadiusMeters;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(String updatedAt) {
        this.updatedAt = updatedAt;
    }
}