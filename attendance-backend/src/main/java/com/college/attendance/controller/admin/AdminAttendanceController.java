package com.college.attendance.controller.admin;

import com.college.attendance.dto.attendance.AttendanceRequest;
import com.college.attendance.dto.attendance.AttendanceResponse;
import com.college.attendance.dto.attendance.AttendanceUserResponse;
import com.college.attendance.service.AttendanceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/admin/attendance")
@RequiredArgsConstructor
public class AdminAttendanceController {

    private final AttendanceService attendanceService;

    /**
     * Create attendance
     */
    @PostMapping
    public ResponseEntity<AttendanceResponse> createAttendance(
            @Valid @RequestBody AttendanceRequest request) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(attendanceService.createAttendance(request));
    }

    /**
     * Get attendance by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<AttendanceResponse> getAttendance(
            @PathVariable Long id) {

        return ResponseEntity.ok(
                attendanceService.getAttendance(id));
    }

    /**
     * Get attendance by user
     * Works for both STUDENT and STAFF
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<AttendanceResponse>> getUserAttendance(
            @PathVariable Long userId) {

        return ResponseEntity.ok(
                attendanceService.getUserAttendance(userId));
    }

    /**
     * Get attendance by date
     */
    @GetMapping("/date/{date}")
    public ResponseEntity<List<AttendanceResponse>> getAttendanceByDate(
            @PathVariable LocalDate date) {

        return ResponseEntity.ok(
                attendanceService.getAttendanceByDate(date));
    }

    /**
     * Get users for attendance
     */
    @GetMapping("/users")
    public ResponseEntity<List<AttendanceUserResponse>> getAttendanceUsers(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long courseId,
            @RequestParam(required = false) Long semesterId,
            @RequestParam(required = false) Long sectionId,
            @RequestParam(required = false) Long departmentId) {

        return ResponseEntity.ok(
                attendanceService.getAttendanceUsers(
                        type,
                        search,
                        courseId,
                        semesterId,
                        sectionId,
                        departmentId));
    }
}