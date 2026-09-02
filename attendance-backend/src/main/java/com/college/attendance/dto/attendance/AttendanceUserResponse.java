package com.college.attendance.dto.attendance;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AttendanceUserResponse {

    private Long userId;

    private String name;

    /**
     * STUDENT / STAFF
     */
    private String type;

    // Student details
    private String rollNumber;

    private String courseName;

    private String courseCode;

    private Integer semester;

    private String sectionName;

    // Common / Staff details
    private String departmentName;

    private String employeeCode;
}