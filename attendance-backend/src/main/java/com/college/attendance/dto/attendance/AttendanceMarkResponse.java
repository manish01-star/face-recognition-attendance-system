package com.college.attendance.dto.attendance;

import com.college.attendance.entity.enums.AttendanceSource;
import com.college.attendance.entity.enums.AttendanceStatus;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AttendanceMarkResponse {

    private boolean success;

    private String action;

    private String message;

    private Long userId;

    private String username;

    private LocalDate attendanceDate;

    private LocalTime checkInTime;

    private LocalTime checkOutTime;

    private AttendanceStatus status;

    private Double confidence;

    private Double distance;

    private Double threshold;

    private Double latitude;

    private Double longitude;

    private String imageUrl;

    private AttendanceSource source;
}