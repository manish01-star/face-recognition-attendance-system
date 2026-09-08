package com.college.attendance.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.college.attendance.entity.enums.Status;

import jakarta.persistence.*;

@Entity
@Table(name = "college_attendance_policy")
public class AttendancePolicy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "policy_name", nullable = false, length = 100)
    private String policyName;

    @Column(name = "saturday_off", nullable = false)
    private Boolean saturdayOff = false;

    @Column(name = "sunday_off", nullable = false)
    private Boolean sundayOff = true;

    @Column(name = "working_hours", nullable = false, precision = 5, scale = 2)
    private BigDecimal workingHours = BigDecimal.valueOf(6.00);

    @Column(name = "attendance_required", nullable = false)
    private Boolean attendanceRequired = true;

    @Column(name = "location_name", nullable = false, length = 150)
    private String locationName;

    @Column(name = "latitude", nullable = false, precision = 10, scale = 7)
    private BigDecimal latitude;

    @Column(name = "longitude", nullable = false, precision = 10, scale = 7)
    private BigDecimal longitude;

    @Column(name = "allowed_radius_meters", nullable = false, precision = 10, scale = 2)
    private BigDecimal allowedRadiusMeters = BigDecimal.valueOf(200.00);

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private Status status = Status.ACTIVE;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;


    @PrePersist
    protected void onCreate() {

        LocalDateTime now = LocalDateTime.now();

        createdAt = now;
        updatedAt = now;

    }


    @PreUpdate
    protected void onUpdate() {

        updatedAt = LocalDateTime.now();

    }


    // =========================================================
    // GETTERS / SETTERS
    // =========================================================

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

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}