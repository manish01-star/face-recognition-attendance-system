package com.college.attendance.controller.admin;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.college.attendance.dto.policy.AttendancePolicyRequest;
import com.college.attendance.dto.policy.AttendancePolicyResponse;
import com.college.attendance.service.AttendancePolicyService;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;

@RestController
@RequestMapping("/api/attendance-policies")
@SecurityRequirement(name = "bearerAuth")
public class AttendancePolicyController {

    private final AttendancePolicyService attendancePolicyService;

    public AttendancePolicyController(
            AttendancePolicyService attendancePolicyService) {

        this.attendancePolicyService = attendancePolicyService;
    }

    // =========================================================
    // CREATE ATTENDANCE POLICY
    // =========================================================

    @PostMapping
    public ResponseEntity<AttendancePolicyResponse> createPolicy(
            @RequestBody AttendancePolicyRequest request) {

        AttendancePolicyResponse response = attendancePolicyService.createPolicy(request);

        return ResponseEntity.ok(response);
    }

    // =========================================================
    // GET ALL POLICIES - ADMIN
    // =========================================================

    @GetMapping("/admin")
    public ResponseEntity<List<AttendancePolicyResponse>> getAllPolicies() {

        return ResponseEntity.ok(
                attendancePolicyService.getAllPolicies());
    }

    // =========================================================
    // GET ACTIVE POLICIES
    // =========================================================

    @GetMapping
    public ResponseEntity<List<AttendancePolicyResponse>> getActivePolicies() {

        return ResponseEntity.ok(
                attendancePolicyService.getActivePolicies());
    }

    // =========================================================
    // GET CURRENT ACTIVE POLICY
    // =========================================================

    @GetMapping("/current")
    public ResponseEntity<AttendancePolicyResponse> getCurrentPolicy() {

        return ResponseEntity.ok(
                attendancePolicyService.getCurrentPolicy());
    }

    // =========================================================
    // GET POLICY BY ID
    // =========================================================

    @GetMapping("/{id}")
    public ResponseEntity<AttendancePolicyResponse> getPolicyById(
            @PathVariable Long id) {

        return ResponseEntity.ok(
                attendancePolicyService.getPolicyById(id));
    }

    // =========================================================
    // UPDATE POLICY
    // =========================================================

    @PutMapping("/{id}")
    public ResponseEntity<AttendancePolicyResponse> updatePolicy(
            @PathVariable Long id,
            @RequestBody AttendancePolicyRequest request) {

        return ResponseEntity.ok(
                attendancePolicyService.updatePolicy(
                        id,
                        request));
    }

    // =========================================================
    // SOFT DELETE
    // =========================================================

    @DeleteMapping("/{id}")
    public ResponseEntity<AttendancePolicyResponse> deletePolicy(
            @PathVariable Long id) {

        return ResponseEntity.ok(
                attendancePolicyService.deletePolicy(id));
    }

    // =========================================================
    // ACTIVATE POLICY
    // =========================================================

    @PutMapping("/{id}/activate")
    public ResponseEntity<AttendancePolicyResponse> activatePolicy(
            @PathVariable Long id) {

        return ResponseEntity.ok(
                attendancePolicyService.activatePolicy(id));
    }
}