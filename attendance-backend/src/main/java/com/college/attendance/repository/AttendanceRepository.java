package com.college.attendance.repository;

import com.college.attendance.entity.Attendance;
import com.college.attendance.entity.enums.AttendanceStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface AttendanceRepository
                extends JpaRepository<Attendance, Long> {

        Optional<Attendance> findByUserIdAndAttendanceDate(
                        Long userId,
                        LocalDate attendanceDate);

        boolean existsByUserIdAndAttendanceDate(
                        Long userId,
                        LocalDate attendanceDate);

        List<Attendance> findByAttendanceDate(
                        LocalDate attendanceDate);

        List<Attendance> findByUserIdOrderByAttendanceDateDesc(
                        Long userId);

        List<Attendance> findByAttendanceDateOrderByCheckInTimeAsc(
                        LocalDate attendanceDate);

        long countByAttendanceDate(
                        LocalDate attendanceDate);

        long countByAttendanceDateAndStatus(
                        LocalDate attendanceDate,
                        AttendanceStatus status);

        List<Attendance> findTop10ByOrderByAttendanceDateDescCheckInTimeDesc();
}