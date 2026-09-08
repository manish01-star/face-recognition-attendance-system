package com.college.attendance.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.college.attendance.entity.AttendancePolicy;
import com.college.attendance.entity.enums.Status;

public interface AttendancePolicyRepository extends JpaRepository<AttendancePolicy, Long> {

    List<AttendancePolicy> findByStatusOrderByIdDesc(Status status);

    Optional<AttendancePolicy> findFirstByStatusOrderByIdDesc(Status status);

    boolean existsByPolicyNameIgnoreCase(String policyName);
}