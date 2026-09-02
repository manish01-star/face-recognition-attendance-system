package com.college.attendance.service;

import com.college.attendance.dto.dashboard.AdminDashboardResponse;
import com.college.attendance.dto.dashboard.RecentAttendanceResponse;
import com.college.attendance.entity.Attendance;
import com.college.attendance.entity.enums.AttendanceStatus;
import com.college.attendance.repository.AttendanceRepository;
import com.college.attendance.repository.CourseRepository;
import com.college.attendance.repository.DepartmentRepository;
import com.college.attendance.repository.SectionRepository;
import com.college.attendance.repository.SemesterRepository;
import com.college.attendance.repository.StudentRepository;
import com.college.attendance.repository.TeacherRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final StudentRepository studentRepository;
    private final TeacherRepository teacherRepository;
    private final DepartmentRepository departmentRepository;
    private final CourseRepository courseRepository;
    private final SemesterRepository semesterRepository;
    private final SectionRepository sectionRepository;
    private final AttendanceRepository attendanceRepository;

    /**
     * Get complete admin dashboard data.
     */
    @Transactional(readOnly = true)
    public AdminDashboardResponse getDashboard() {

        LocalDate today = LocalDate.now();

        // =====================================================
        // MASTER DATA COUNTS
        // =====================================================

        long totalStudents = studentRepository.count();

        long totalTeachers = teacherRepository.count();

        long totalDepartments = departmentRepository.count();

        long totalCourses = courseRepository.count();

        long totalSemesters = semesterRepository.count();

        long totalSections = sectionRepository.count();

        // =====================================================
        // TODAY ATTENDANCE
        // =====================================================

        long todayAttendance = attendanceRepository
                .countByAttendanceDate(today);

        long todayPresent = attendanceRepository
                .countByAttendanceDateAndStatus(
                        today,
                        AttendanceStatus.PRESENT);

        long todayAbsent = attendanceRepository
                .countByAttendanceDateAndStatus(
                        today,
                        AttendanceStatus.ABSENT);

        // =====================================================
        // RECENT ATTENDANCE
        // =====================================================

        List<RecentAttendanceResponse> recentAttendance = attendanceRepository
                .findTop10ByOrderByAttendanceDateDescCheckInTimeDesc()
                .stream()
                .map(this::mapRecentAttendance)
                .toList();

        // =====================================================
        // RESPONSE
        // =====================================================

        return AdminDashboardResponse.builder()

                .totalStudents(totalStudents)

                .totalTeachers(totalTeachers)

                .totalDepartments(totalDepartments)

                .totalCourses(totalCourses)

                .totalSemesters(totalSemesters)

                .totalSections(totalSections)

                .todayAttendance(todayAttendance)

                .todayPresent(todayPresent)

                .todayAbsent(todayAbsent)

                .recentAttendance(recentAttendance)

                .build();
    }

    // =========================================================
    // MAP ATTENDANCE
    // =========================================================

    private RecentAttendanceResponse mapRecentAttendance(
            Attendance attendance) {

        return RecentAttendanceResponse.builder()

                .userId(
                        attendance.getUser().getId())

                .username(
                        attendance.getUser().getUsername())

                .attendanceDate(
                        attendance.getAttendanceDate())

                .checkInTime(
                        attendance.getCheckInTime())

                .checkOutTime(
                        attendance.getCheckOutTime())

                .status(
                        attendance.getStatus())

                .build();
    }
}