package com.college.attendance.dto.holiday;

import java.time.LocalDate;

import com.college.attendance.entity.enums.HolidayType;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class HolidayRequest {

    private LocalDate holidayDate;

    private String holidayName;

    private String description;

    private HolidayType holidayType;

}