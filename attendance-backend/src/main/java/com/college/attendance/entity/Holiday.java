package com.college.attendance.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.college.attendance.entity.enums.Status;
import com.college.attendance.entity.enums.HolidayType;

import jakarta.persistence.*;

@Entity
@Table(
    name = "holidays",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_holiday_date",
            columnNames = "holiday_date"
        )
    }
)
public class Holiday {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(
        name = "holiday_date",
        nullable = false
    )
    private LocalDate holidayDate;

    @Column(
        name = "holiday_name",
        nullable = false,
        length = 150
    )
    private String holidayName;

    @Column(
        length = 500
    )
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(
        name = "holiday_type",
        nullable = false
    )
    private HolidayType holidayType;

    @Enumerated(EnumType.STRING)
    @Column(
        nullable = false
    )
    private Status status;

    @Column(
        name = "created_at",
        nullable = false
    )
    private LocalDateTime createdAt;

    @Column(
        name = "updated_at",
        nullable = false
    )
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {

        LocalDateTime now = LocalDateTime.now();

        createdAt = now;
        updatedAt = now;

        if (status == null) {
            status = Status.ACTIVE;
        }

        if (holidayType == null) {
            holidayType = HolidayType.COLLEGE;
        }
    }

    @PreUpdate
    protected void onUpdate() {

        updatedAt = LocalDateTime.now();
    }


    // =========================
    // GETTERS & SETTERS
    // =========================

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDate getHolidayDate() {
        return holidayDate;
    }

    public void setHolidayDate(LocalDate holidayDate) {
        this.holidayDate = holidayDate;
    }

    public String getHolidayName() {
        return holidayName;
    }

    public void setHolidayName(String holidayName) {
        this.holidayName = holidayName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public HolidayType getHolidayType() {
        return holidayType;
    }

    public void setHolidayType(HolidayType holidayType) {
        this.holidayType = holidayType;
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