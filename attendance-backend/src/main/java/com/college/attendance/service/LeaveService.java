package com.college.attendance.service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.college.attendance.dto.leave.LeaveApplyRequest;
import com.college.attendance.dto.leave.LeaveResponse;
import com.college.attendance.dto.leave.LeaveReviewRequest;
import com.college.attendance.entity.Leave;
import com.college.attendance.entity.enums.LeaveStatus;
import com.college.attendance.repository.LeaveRepository;

@Service
public class LeaveService {

    private final LeaveRepository leaveRepository;

    public LeaveService(LeaveRepository leaveRepository) {
        this.leaveRepository = leaveRepository;
    }

    // =========================================================
    // USER - APPLY LEAVE
    // =========================================================

    @Transactional
    public LeaveResponse applyLeave(
            Long userId,
            LeaveApplyRequest request) {

        if (userId == null) {
            throw new RuntimeException("User ID is required");
        }

        if (request == null) {
            throw new RuntimeException("Leave request is required");
        }

        if (request.getFromDate() == null) {
            throw new RuntimeException("From date is required");
        }

        if (request.getToDate() == null) {
            throw new RuntimeException("To date is required");
        }

        if (request.getLeaveType() == null) {
            throw new RuntimeException("Leave type is required");
        }

        if (request.getDescription() == null
                || request.getDescription().trim().isEmpty()) {

            throw new RuntimeException(
                    "Leave description is required");
        }

        if (request.getDescription().trim().length() > 500) {
            throw new RuntimeException(
                    "Leave description cannot exceed 500 characters");
        }

        // From date cannot be after To date
        if (request.getFromDate().isAfter(request.getToDate())) {
            throw new RuntimeException(
                    "From date cannot be after to date");
        }

        // Past leave not allowed
        if (request.getToDate().isBefore(LocalDate.now())) {
            throw new RuntimeException(
                    "Leave date cannot be in the past");
        }

        // Weekend-only leave not required
        if (isWeekendOnly(
                request.getFromDate(),
                request.getToDate())) {

            throw new RuntimeException(
                    "Leave is not required for Saturday and Sunday");
        }

        // =====================================================
        // CHECK PENDING OVERLAP
        // =====================================================

        boolean pendingExists = leaveRepository
                .existsByUserIdAndStatusAndFromDateLessThanEqualAndToDateGreaterThanEqual(
                        userId,
                        LeaveStatus.PENDING,
                        request.getToDate(),
                        request.getFromDate());

        if (pendingExists) {
            throw new RuntimeException(
                    "You already have a pending leave for these dates");
        }

        // =====================================================
        // CHECK APPROVED OVERLAP
        // =====================================================

        boolean approvedExists = leaveRepository
                .existsByUserIdAndStatusAndFromDateLessThanEqualAndToDateGreaterThanEqual(
                        userId,
                        LeaveStatus.APPROVED,
                        request.getToDate(),
                        request.getFromDate());

        if (approvedExists) {
            throw new RuntimeException(
                    "You already have an approved leave for these dates");
        }

        // =====================================================
        // CREATE LEAVE
        // =====================================================

        Leave leave = new Leave();

        // Copy matching fields from DTO -> Entity
        BeanUtils.copyProperties(request, leave);

        // Server-controlled fields
        leave.setUserId(userId);
        leave.setDescription(request.getDescription().trim());
        leave.setStatus(LeaveStatus.PENDING);
        leave.setAppliedAt(LocalDateTime.now());

        Leave savedLeave = leaveRepository.save(leave);

        return mapToResponse(savedLeave);
    }

    // =========================================================
    // USER - GET MY LEAVES
    // =========================================================

    public List<LeaveResponse> getMyLeaves(Long userId) {

        if (userId == null) {
            throw new RuntimeException("User ID is required");
        }

        return leaveRepository
                .findByUserIdOrderByFromDateDesc(userId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    // =========================================================
    // USER - GET MY UPCOMING LEAVES
    // =========================================================

    public List<LeaveResponse> getMyUpcomingLeaves(Long userId) {

        if (userId == null) {
            throw new RuntimeException("User ID is required");
        }

        LocalDate today = LocalDate.now();

        return leaveRepository
                .findByUserIdOrderByFromDateDesc(userId)
                .stream()
                .filter(leave ->
                        !leave.getToDate().isBefore(today))
                .map(this::mapToResponse)
                .toList();
    }

    // =========================================================
    // USER / ADMIN - GET LEAVE BY ID
    // =========================================================

    public LeaveResponse getLeaveById(Long id) {

        Leave leave = getLeaveEntity(id);

        return mapToResponse(leave);
    }

    // =========================================================
    // ADMIN - GET ALL LEAVES
    // =========================================================

    public List<LeaveResponse> getAllLeaves() {

        return leaveRepository
                .findAllByOrderByAppliedAtDesc()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    // =========================================================
    // ADMIN - GET PENDING LEAVES
    // =========================================================

    public List<LeaveResponse> getPendingLeaves() {

        return leaveRepository
                .findByStatusOrderByAppliedAtDesc(
                        LeaveStatus.PENDING)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    // =========================================================
    // ADMIN - APPROVE LEAVE
    // =========================================================

    @Transactional
    public LeaveResponse approveLeave(
            Long leaveId,
            Long adminId,
            LeaveReviewRequest request) {

        if (adminId == null) {
            throw new RuntimeException(
                    "Admin ID is required");
        }

        Leave leave = getLeaveEntity(leaveId);

        if (leave.getStatus() != LeaveStatus.PENDING) {
            throw new RuntimeException(
                    "Only pending leave can be approved");
        }

        // =====================================================
        // APPROVE
        // =====================================================

        leave.setStatus(LeaveStatus.APPROVED);
        leave.setReviewedBy(adminId);
        leave.setReviewedAt(LocalDateTime.now());

        // Admin remark
        if (request != null
                && request.getAdminRemark() != null) {

            String remark = request.getAdminRemark().trim();

            if (remark.length() > 500) {
                throw new RuntimeException(
                        "Admin remark cannot exceed 500 characters");
            }

            leave.setAdminRemark(remark);
        }

        Leave savedLeave = leaveRepository.save(leave);

        return mapToResponse(savedLeave);
    }

    // =========================================================
    // ADMIN - REJECT LEAVE
    // =========================================================

    @Transactional
    public LeaveResponse rejectLeave(
            Long leaveId,
            Long adminId,
            LeaveReviewRequest request) {

        if (adminId == null) {
            throw new RuntimeException(
                    "Admin ID is required");
        }

        Leave leave = getLeaveEntity(leaveId);

        if (leave.getStatus() != LeaveStatus.PENDING) {
            throw new RuntimeException(
                    "Only pending leave can be rejected");
        }

        // =====================================================
        // REJECT
        // =====================================================

        leave.setStatus(LeaveStatus.REJECTED);
        leave.setReviewedBy(adminId);
        leave.setReviewedAt(LocalDateTime.now());

        // Admin remark
        if (request != null
                && request.getAdminRemark() != null) {

            String remark = request.getAdminRemark().trim();

            if (remark.length() > 500) {
                throw new RuntimeException(
                        "Admin remark cannot exceed 500 characters");
            }

            leave.setAdminRemark(remark);
        }

        Leave savedLeave = leaveRepository.save(leave);

        return mapToResponse(savedLeave);
    }

    // =========================================================
    // PRIVATE - GET ENTITY
    // =========================================================

    private Leave getLeaveEntity(Long id) {

        if (id == null) {
            throw new RuntimeException(
                    "Leave ID is required");
        }

        return leaveRepository
                .findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Leave not found"));
    }

    // =========================================================
    // PRIVATE - WEEKEND CHECK
    // =========================================================

    private boolean isWeekendOnly(
            LocalDate fromDate,
            LocalDate toDate) {

        LocalDate date = fromDate;

        while (!date.isAfter(toDate)) {

            DayOfWeek day = date.getDayOfWeek();

            if (day != DayOfWeek.SATURDAY
                    && day != DayOfWeek.SUNDAY) {

                return false;
            }

            date = date.plusDays(1);
        }

        return true;
    }

    // =========================================================
    // PRIVATE - ENTITY TO RESPONSE
    // =========================================================

    private LeaveResponse mapToResponse(Leave leave) {

        LeaveResponse response = new LeaveResponse();

        // Entity -> Response
        BeanUtils.copyProperties(leave, response);

        return response;
    }
}