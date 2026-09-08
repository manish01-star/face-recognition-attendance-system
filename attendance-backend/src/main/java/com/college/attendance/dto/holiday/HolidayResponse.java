package com.college.attendance.dto.holiday;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.college.attendance.entity.enums.Status;
import com.college.attendance.entity.enums.HolidayType;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class HolidayResponse {

    private Long id;

    private LocalDate holidayDate;

    private String holidayName;

    private String description;

    private HolidayType holidayType;

    private Status status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

}