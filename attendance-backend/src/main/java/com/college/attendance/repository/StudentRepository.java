package com.college.attendance.repository;

import com.college.attendance.entity.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface StudentRepository
                extends JpaRepository<Student, Long> {

        Optional<Student> findByRollNumber(String rollNumber);

        boolean existsByRollNumber(String rollNumber);

        Optional<Student> findByUserId(Long userId);

        boolean existsByUserId(Long userId);

        /**
         * Dynamic attendance user search.
         */
        @Query("""
                        SELECT s
                        FROM Student s
                        JOIN FETCH s.user u
                        JOIN FETCH s.course c
                        JOIN FETCH s.semester sem
                        JOIN FETCH s.section sec
                        LEFT JOIN FETCH c.department d
                        WHERE u.status = com.college.attendance.entity.enums.UserStatus.ACTIVE
                        AND (
                            :search IS NULL
                            OR LOWER(s.name) LIKE LOWER(CONCAT('%', :search, '%'))
                            OR LOWER(s.rollNumber) LIKE LOWER(CONCAT('%', :search, '%'))
                            OR CAST(u.id AS string) LIKE CONCAT('%', :search, '%')
                        )
                        AND (:courseId IS NULL OR c.id = :courseId)
                        AND (:semesterId IS NULL OR sem.id = :semesterId)
                        AND (:sectionId IS NULL OR sec.id = :sectionId)
                        AND (:departmentId IS NULL OR d.id = :departmentId)
                        ORDER BY LOWER(s.name) ASC
                        """)
        List<Student> searchAttendanceStudents(
                        @Param("search") String search,
                        @Param("courseId") Long courseId,
                        @Param("semesterId") Long semesterId,
                        @Param("sectionId") Long sectionId,
                        @Param("departmentId") Long departmentId);

        @Query("""
                        SELECT s
                        FROM Student s
                        JOIN FETCH s.user u
                        WHERE s.user.id = :userId
                        """)
        Optional<Student> findProfileByUserId(
                        @Param("userId") Long userId);
}