package com.college.attendance.dto.dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminDashboardResponse {

    private long totalStudents;

    private long totalTeachers;

    private long totalDepartments;

    private long totalCourses;

    private long totalSemesters;

    private long totalSections;

    private long todayAttendance;

    private long todayPresent;

    private long todayAbsent;

    private List<RecentAttendanceResponse> recentAttendance;
}