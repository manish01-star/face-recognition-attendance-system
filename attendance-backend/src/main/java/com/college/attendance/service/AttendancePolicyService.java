package com.college.attendance.service;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.college.attendance.dto.policy.AttendancePolicyRequest;
import com.college.attendance.dto.policy.AttendancePolicyResponse;
import com.college.attendance.entity.AttendancePolicy;
import com.college.attendance.entity.enums.Status;
import com.college.attendance.repository.AttendancePolicyRepository;

@Service
public class AttendancePolicyService {

    private final AttendancePolicyRepository attendancePolicyRepository;

    public AttendancePolicyService(
            AttendancePolicyRepository attendancePolicyRepository) {
        this.attendancePolicyRepository = attendancePolicyRepository;
    }

    // =========================================================
    // CREATE POLICY
    // =========================================================

    @Transactional
    public AttendancePolicyResponse createPolicy(
            AttendancePolicyRequest request) {

        validateRequest(request);

        if (attendancePolicyRepository
                .existsByPolicyNameIgnoreCase(request.getPolicyName().trim())) {

            throw new RuntimeException(
                    "Attendance policy with this name already exists");
        }

        AttendancePolicy policy = new AttendancePolicy();

        BeanUtils.copyProperties(request, policy);

        policy.setPolicyName(request.getPolicyName().trim());
        policy.setLocationName(request.getLocationName().trim());

        if (policy.getStatus() == null) {
            policy.setStatus(Status.ACTIVE);
        }

        /*
         * Only one active attendance policy should exist.
         */
        if (policy.getStatus() == Status.ACTIVE) {
            deactivateAllActivePolicies();
        }

        AttendancePolicy savedPolicy =
                attendancePolicyRepository.save(policy);

        return mapToResponse(savedPolicy);
    }

    // =========================================================
    // GET ALL POLICIES - ADMIN
    // =========================================================

    public List<AttendancePolicyResponse> getAllPolicies() {

        return attendancePolicyRepository
                .findAll()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    // =========================================================
    // GET ACTIVE POLICIES
    // =========================================================

    public List<AttendancePolicyResponse> getActivePolicies() {

        return attendancePolicyRepository
                .findByStatusOrderByIdDesc(Status.ACTIVE)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    // =========================================================
    // GET CURRENT ACTIVE POLICY
    // =========================================================

    public AttendancePolicyResponse getCurrentPolicy() {

        AttendancePolicy policy =
                attendancePolicyRepository
                        .findFirstByStatusOrderByIdDesc(Status.ACTIVE)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "No active attendance policy found"));

        return mapToResponse(policy);
    }

    // =========================================================
    // GET POLICY BY ID
    // =========================================================

    public AttendancePolicyResponse getPolicyById(Long id) {

        AttendancePolicy policy = getPolicyEntity(id);

        return mapToResponse(policy);
    }

    // =========================================================
    // UPDATE POLICY
    // =========================================================

    @Transactional
    public AttendancePolicyResponse updatePolicy(
            Long id,
            AttendancePolicyRequest request) {

        validateRequest(request);

        AttendancePolicy policy =
                getPolicyEntity(id);

        /*
         * Check duplicate policy name
         */
        boolean duplicateName =
                attendancePolicyRepository
                        .existsByPolicyNameIgnoreCase(
                                request.getPolicyName().trim());

        if (duplicateName
                && !policy.getPolicyName()
                        .equalsIgnoreCase(request.getPolicyName().trim())) {

            throw new RuntimeException(
                    "Attendance policy with this name already exists");
        }

        BeanUtils.copyProperties(request, policy);

        policy.setPolicyName(
                request.getPolicyName().trim());

        policy.setLocationName(
                request.getLocationName().trim());

        if (policy.getStatus() == null) {
            policy.setStatus(Status.ACTIVE);
        }

        /*
         * If updated policy is ACTIVE,
         * deactivate all other active policies.
         */
        if (policy.getStatus() == Status.ACTIVE) {
            deactivateOtherActivePolicies(id);
        }

        AttendancePolicy updatedPolicy =
                attendancePolicyRepository.save(policy);

        return mapToResponse(updatedPolicy);
    }

    // =========================================================
    // SOFT DELETE
    // =========================================================

    @Transactional
    public AttendancePolicyResponse deletePolicy(Long id) {

        AttendancePolicy policy =
                getPolicyEntity(id);

        if (policy.getStatus() == Status.INACTIVE) {
            throw new RuntimeException(
                    "Attendance policy is already inactive");
        }

        policy.setStatus(Status.INACTIVE);

        AttendancePolicy updatedPolicy =
                attendancePolicyRepository.save(policy);

        return mapToResponse(updatedPolicy);
    }

    // =========================================================
    // ACTIVATE POLICY
    // =========================================================

    @Transactional
    public AttendancePolicyResponse activatePolicy(Long id) {

        AttendancePolicy policy =
                getPolicyEntity(id);

        if (policy.getStatus() == Status.ACTIVE) {
            throw new RuntimeException(
                    "Attendance policy is already active");
        }

        /*
         * Only one active policy
         */
        deactivateAllActivePolicies();

        policy.setStatus(Status.ACTIVE);

        AttendancePolicy activatedPolicy =
                attendancePolicyRepository.save(policy);

        return mapToResponse(activatedPolicy);
    }

    // =========================================================
    // VALIDATION
    // =========================================================

    private void validateRequest(
            AttendancePolicyRequest request) {

        if (request == null) {
            throw new RuntimeException(
                    "Attendance policy request is required");
        }

        // Policy name
        if (request.getPolicyName() == null
                || request.getPolicyName().trim().isEmpty()) {

            throw new RuntimeException(
                    "Policy name is required");
        }

        if (request.getPolicyName().trim().length() > 100) {

            throw new RuntimeException(
                    "Policy name cannot exceed 100 characters");
        }

        // Working hours
        if (request.getWorkingHours() == null) {

            throw new RuntimeException(
                    "Working hours are required");
        }

        if (request.getWorkingHours()
                .compareTo(BigDecimal.ZERO) <= 0) {

            throw new RuntimeException(
                    "Working hours must be greater than 0");
        }

        if (request.getWorkingHours()
                .compareTo(BigDecimal.valueOf(24)) > 0) {

            throw new RuntimeException(
                    "Working hours cannot exceed 24");
        }

        // Location name
        if (request.getLocationName() == null
                || request.getLocationName().trim().isEmpty()) {

            throw new RuntimeException(
                    "Location name is required");
        }

        if (request.getLocationName().trim().length() > 150) {

            throw new RuntimeException(
                    "Location name cannot exceed 150 characters");
        }

        // Latitude
        if (request.getLatitude() == null) {

            throw new RuntimeException(
                    "Latitude is required");
        }

        if (request.getLatitude()
                .compareTo(BigDecimal.valueOf(-90)) < 0
                || request.getLatitude()
                        .compareTo(BigDecimal.valueOf(90)) > 0) {

            throw new RuntimeException(
                    "Latitude must be between -90 and 90");
        }

        // Longitude
        if (request.getLongitude() == null) {

            throw new RuntimeException(
                    "Longitude is required");
        }

        if (request.getLongitude()
                .compareTo(BigDecimal.valueOf(-180)) < 0
                || request.getLongitude()
                        .compareTo(BigDecimal.valueOf(180)) > 0) {

            throw new RuntimeException(
                    "Longitude must be between -180 and 180");
        }

        // Radius
        if (request.getAllowedRadiusMeters() == null) {

            throw new RuntimeException(
                    "Allowed radius is required");
        }

        if (request.getAllowedRadiusMeters()
                .compareTo(BigDecimal.ZERO) <= 0) {

            throw new RuntimeException(
                    "Allowed radius must be greater than 0");
        }

        if (request.getAllowedRadiusMeters()
                .compareTo(BigDecimal.valueOf(10000)) > 0) {

            throw new RuntimeException(
                    "Allowed radius cannot exceed 10000 meters");
        }

        // Attendance required
        if (request.getAttendanceRequired() == null) {
            request.setAttendanceRequired(true);
        }

        // Saturday off
        if (request.getSaturdayOff() == null) {
            request.setSaturdayOff(false);
        }

        // Sunday off
        if (request.getSundayOff() == null) {
            request.setSundayOff(true);
        }

        // Status
        if (request.getStatus() == null) {
            request.setStatus(Status.ACTIVE);
        }
    }

    // =========================================================
    // DEACTIVATE ALL ACTIVE POLICIES
    // =========================================================

    private void deactivateAllActivePolicies() {

        List<AttendancePolicy> activePolicies =
                attendancePolicyRepository
                        .findByStatusOrderByIdDesc(Status.ACTIVE);

        for (AttendancePolicy policy : activePolicies) {
            policy.setStatus(Status.INACTIVE);
        }

        attendancePolicyRepository.saveAll(activePolicies);
    }

    // =========================================================
    // DEACTIVATE OTHER ACTIVE POLICIES
    // =========================================================

    private void deactivateOtherActivePolicies(Long currentId) {

        List<AttendancePolicy> activePolicies =
                attendancePolicyRepository
                        .findByStatusOrderByIdDesc(Status.ACTIVE);

        for (AttendancePolicy policy : activePolicies) {

            if (!policy.getId().equals(currentId)) {
                policy.setStatus(Status.INACTIVE);
            }
        }

        attendancePolicyRepository.saveAll(activePolicies);
    }

    // =========================================================
    // GET ENTITY
    // =========================================================

    private AttendancePolicy getPolicyEntity(Long id) {

        if (id == null) {
            throw new RuntimeException(
                    "Attendance policy ID is required");
        }

        return attendancePolicyRepository
                .findById(id)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Attendance policy not found"));
    }

    // =========================================================
    // ENTITY -> RESPONSE
    // =========================================================

    private AttendancePolicyResponse mapToResponse(
            AttendancePolicy policy) {

        AttendancePolicyResponse response =
                new AttendancePolicyResponse();

        BeanUtils.copyProperties(policy, response);

        return response;
    }
}