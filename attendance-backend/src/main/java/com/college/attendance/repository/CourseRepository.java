package com.college.attendance.repository;

import com.college.attendance.entity.Course;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CourseRepository extends JpaRepository<Course, Long> {

    Optional<Course> findByCode(String code);

    boolean existsByCode(String code);

    List<Course> findByDepartmentId(Long departmentId);
}