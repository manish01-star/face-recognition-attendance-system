package com.college.attendance.controller.admin;

import com.college.attendance.dto.dashboard.AdminDashboardResponse;
import com.college.attendance.service.DashboardService;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/dashboard")
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
public class AdminDashboardController {

    private final DashboardService dashboardService;

    /**
     * Admin Dashboard
     *
     * Returns:
     * - Total students
     * - Total teachers
     * - Total departments
     * - Total courses
     * - Total semesters
     * - Total sections
     * - Today's attendance
     * - Today's present
     * - Today's absent
     * - Recent attendance
     */
    @GetMapping
    public ResponseEntity<AdminDashboardResponse> getDashboard() {

        return ResponseEntity.ok(
                dashboardService.getDashboard()
        );
    }
}