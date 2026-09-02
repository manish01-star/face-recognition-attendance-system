package com.college.attendance.repository;

import com.college.attendance.entity.StudentFaceProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StudentFaceProfileRepository extends JpaRepository<StudentFaceProfile, Long> {

    Optional<StudentFaceProfile> findByStudentId(Long studentId);

    boolean existsByStudentId(Long studentId);

    void deleteByStudentId(Long studentId);
}