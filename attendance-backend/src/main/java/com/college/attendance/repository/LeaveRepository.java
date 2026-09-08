package com.college.attendance.repository;

import com.college.attendance.entity.Leave;
import com.college.attendance.entity.enums.LeaveStatus;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface LeaveRepository
        extends JpaRepository<Leave, Long> {

    // User's all leaves
    List<Leave> findByUserIdOrderByFromDateDesc(
            Long userId);

    // User's leaves by status
    List<Leave> findByUserIdAndStatusOrderByFromDateDesc(
            Long userId,
            LeaveStatus status);

    // Admin - all leaves
    List<Leave> findAllByOrderByAppliedAtDesc();

    // Admin - pending/approved/rejected
    List<Leave> findByStatusOrderByAppliedAtDesc(
            LeaveStatus status);

    // Check overlapping leave
    boolean existsByUserIdAndStatusAndFromDateLessThanEqualAndToDateGreaterThanEqual(
            Long userId,
            LeaveStatus status,
            LocalDate toDate,
            LocalDate fromDate);
}