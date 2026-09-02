package com.college.attendance.dto.attendance;

import com.college.attendance.entity.enums.AttendanceSource;
import com.college.attendance.entity.enums.AttendanceStatus;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AttendanceResponse {

    private Long id;

    private Long userId;

    private String username;

    private LocalDate attendanceDate;

    private LocalTime checkInTime;

    private LocalTime checkOutTime;

    private AttendanceStatus status;

    private Double confidence;

    private Double checkInLatitude;

    private Double checkInLongitude;

    private Double checkOutLatitude;

    private Double checkOutLongitude;

    private AttendanceSource source;

    private LocalDateTime createdAt;
}