package com.college.attendance.dto.attendance;

import com.college.attendance.entity.enums.AttendanceSource;
import com.college.attendance.entity.enums.AttendanceStatus;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AttendanceRequest {

    @NotNull(message = "User ID is required")
    private Long userId;

    @NotNull(message = "Attendance date is required")
    private LocalDate attendanceDate;

    private LocalTime checkInTime;

    private LocalTime checkOutTime;

    @NotNull(message = "Attendance status is required")
    private AttendanceStatus status;

    private Double confidence;

    private Double checkInLatitude;

    private Double checkInLongitude;

    private Double checkOutLatitude;

    private Double checkOutLongitude;

    private AttendanceSource source;
}