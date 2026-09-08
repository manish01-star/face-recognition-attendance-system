package com.college.attendance.repository;

import com.college.attendance.entity.Teacher;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TeacherRepository
                extends JpaRepository<Teacher, Long> {

        Optional<Teacher> findByEmployeeCode(String employeeCode);

        boolean existsByEmployeeCode(String employeeCode);

        Optional<Teacher> findByUserId(Long userId);

        boolean existsByUserId(Long userId);

        /**
         * Dynamic attendance staff search.
         */
        @Query("""
                        SELECT t
                        FROM Teacher t
                        JOIN FETCH t.user u
                        LEFT JOIN FETCH t.department d
                        WHERE u.status = com.college.attendance.entity.enums.UserStatus.ACTIVE
                        AND (
                            :search IS NULL
                            OR LOWER(t.name) LIKE LOWER(CONCAT('%', :search, '%'))
                            OR LOWER(t.employeeCode) LIKE LOWER(CONCAT('%', :search, '%'))
                            OR CAST(u.id AS string) LIKE CONCAT('%', :search, '%')
                        )
                        AND (:departmentId IS NULL OR d.id = :departmentId)
                        ORDER BY LOWER(t.name) ASC
                        """)
        List<Teacher> searchAttendanceTeachers(
                        @Param("search") String search,
                        @Param("departmentId") Long departmentId);

        @Query("""
                        SELECT t
                        FROM Teacher t
                        JOIN FETCH t.user u
                        WHERE t.user.id = :userId
                        """)
        Optional<Teacher> findProfileByUserId(
                        @Param("userId") Long userId);
}