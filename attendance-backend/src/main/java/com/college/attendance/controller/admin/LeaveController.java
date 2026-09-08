package com.college.attendance.controller.admin;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.college.attendance.dto.leave.LeaveApplyRequest;
import com.college.attendance.dto.leave.LeaveResponse;
import com.college.attendance.dto.leave.LeaveReviewRequest;
import com.college.attendance.service.LeaveService;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;

@RestController
@RequestMapping("/api/leaves")
@SecurityRequirement(name = "bearerAuth")
public class LeaveController {

    private final LeaveService leaveService;

    public LeaveController(LeaveService leaveService) {
        this.leaveService = leaveService;
    }

    // =========================================================
    // USER - APPLY LEAVE
    // =========================================================

    @PostMapping
    public ResponseEntity<LeaveResponse> applyLeave(

            @RequestAttribute("userId") Long userId,

            @RequestBody LeaveApplyRequest request) {

        LeaveResponse response = leaveService.applyLeave(
                userId,
                request);

        return ResponseEntity.ok(response);
    }

    // =========================================================
    // USER - MY LEAVES
    // =========================================================

    @GetMapping("/my")
    public ResponseEntity<List<LeaveResponse>> getMyLeaves(

            @RequestAttribute("userId") Long userId) {

        return ResponseEntity.ok(
                leaveService.getMyLeaves(userId));
    }

    // =========================================================
    // USER - MY UPCOMING LEAVES
    // =========================================================

    @GetMapping("/my/upcoming")
    public ResponseEntity<List<LeaveResponse>> getMyUpcomingLeaves(

            @RequestAttribute("userId") Long userId) {

        return ResponseEntity.ok(
                leaveService.getMyUpcomingLeaves(userId));
    }

    // =========================================================
    // USER - GET LEAVE BY ID
    // =========================================================

    @GetMapping("/{id}")
    public ResponseEntity<LeaveResponse> getLeaveById(

            @PathVariable Long id) {

        return ResponseEntity.ok(
                leaveService.getLeaveById(id));
    }

    // =========================================================
    // ADMIN - GET ALL LEAVES
    // =========================================================

    @GetMapping("/admin")
    public ResponseEntity<List<LeaveResponse>> getAllLeaves() {

        return ResponseEntity.ok(
                leaveService.getAllLeaves());
    }

    // =========================================================
    // ADMIN - GET PENDING LEAVES
    // =========================================================

    @GetMapping("/admin/pending")
    public ResponseEntity<List<LeaveResponse>> getPendingLeaves() {

        return ResponseEntity.ok(
                leaveService.getPendingLeaves());
    }

    // =========================================================
    // ADMIN - APPROVE LEAVE
    // =========================================================

    @PutMapping("/admin/{id}/approve")
    public ResponseEntity<LeaveResponse> approveLeave(

            @PathVariable Long id,

            @RequestAttribute("userId") Long adminId,

            @RequestBody(required = false) LeaveReviewRequest request) {

        LeaveResponse response = leaveService.approveLeave(
                id,
                adminId,
                request);

        return ResponseEntity.ok(response);
    }

    // =========================================================
    // ADMIN - REJECT LEAVE
    // =========================================================

    @PutMapping("/admin/{id}/reject")
    public ResponseEntity<LeaveResponse> rejectLeave(

            @PathVariable Long id,

            @RequestAttribute("userId") Long adminId,

            @RequestBody(required = false) LeaveReviewRequest request) {

        LeaveResponse response = leaveService.rejectLeave(
                id,
                adminId,
                request);

        return ResponseEntity.ok(response);
    }
}