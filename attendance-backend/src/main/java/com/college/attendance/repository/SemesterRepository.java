package com.college.attendance.repository;

import com.college.attendance.entity.Semester;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SemesterRepository extends JpaRepository<Semester, Long> {

    List<Semester> findByCourseId(Long courseId);

    Optional<Semester> findByCourseIdAndSemesterNumber(
            Long courseId,
            Integer semesterNumber
    );

    boolean existsByCourseIdAndSemesterNumber(
            Long courseId,
            Integer semesterNumber
    );
}