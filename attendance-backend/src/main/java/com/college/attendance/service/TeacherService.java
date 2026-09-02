package com.college.attendance.service;

import com.college.attendance.dto.teacher.TeacherRequest;
import com.college.attendance.dto.teacher.TeacherResponse;
import com.college.attendance.entity.Department;
import com.college.attendance.entity.Teacher;
import com.college.attendance.entity.User;
import com.college.attendance.entity.enums.Role;
import com.college.attendance.entity.enums.UserStatus;
import com.college.attendance.repository.DepartmentRepository;
import com.college.attendance.repository.TeacherRepository;
import com.college.attendance.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TeacherService {

        private final TeacherRepository teacherRepository;
        private final UserRepository userRepository;
        private final DepartmentRepository departmentRepository;
        private final PasswordEncoder passwordEncoder;

        /**
         * Create or Update Teacher
         *
         * id == null -> Create
         * id != null -> Update
         */
        @Transactional
        public TeacherResponse createOrUpdate(
                        Long id,
                        TeacherRequest request) {

                Teacher teacher;

                // ==========================================
                // CREATE
                // ==========================================

                if (id == null) {

                        if (teacherRepository.existsByEmployeeCode(
                                        request.getEmployeeCode())) {

                                throw new RuntimeException(
                                                "Teacher with this employee code already exists");
                        }

                        if (userRepository.existsByUsername(
                                        request.getEmployeeCode())) {

                                throw new RuntimeException(
                                                "Username already exists");
                        }

                        // --------------------------------------
                        // Department
                        // --------------------------------------

                        Department department = departmentRepository.findById(
                                        request.getDepartmentId()).orElseThrow(
                                                        () -> new RuntimeException(
                                                                        "Department not found"));

                        // --------------------------------------
                        // User
                        // --------------------------------------

                        User user = User.builder()
                                        .username(request.getEmployeeCode())
                                        .password(
                                                        passwordEncoder.encode(
                                                                        request.getPhone()))
                                        .role(Role.TEACHER)
                                        .status(UserStatus.ACTIVE)
                                        .build();

                        userRepository.save(user);

                        // --------------------------------------
                        // Teacher
                        // --------------------------------------

                        teacher = new Teacher();

                        teacher.setUser(user);

                        BeanUtils.copyProperties(
                                        request,
                                        teacher);

                        teacher.setDepartment(department);
                }

                // ==========================================
                // UPDATE
                // ==========================================

                else {

                        teacher = teacherRepository.findById(id)
                                        .orElseThrow(() -> new RuntimeException(
                                                        "Teacher not found"));

                        User user = teacher.getUser();

                        Department department = departmentRepository.findById(
                                        request.getDepartmentId()).orElseThrow(
                                                        () -> new RuntimeException(
                                                                        "Department not found"));

                        BeanUtils.copyProperties(
                                        request,
                                        teacher);

                        teacher.setDepartment(department);

                        /*
                         * Phone number is password.
                         * Update password if phone changed.
                         */
                        if (!passwordEncoder.matches(
                                        request.getPhone(),
                                        user.getPassword())) {

                                user.setPassword(
                                                passwordEncoder.encode(
                                                                request.getPhone()));

                                userRepository.save(user);
                        }
                }

                teacher = teacherRepository.save(teacher);

                return toResponse(teacher);
        }

        /**
         * Get all teachers
         */
        @Transactional(readOnly = true)
        public List<TeacherResponse> getAll() {

                return teacherRepository.findAll()
                                .stream()
                                .map(this::toResponse)
                                .toList();
        }

        /**
         * Get teacher by ID
         */
        @Transactional(readOnly = true)
        public TeacherResponse getById(Long id) {

                Teacher teacher = teacherRepository.findById(id)
                                .orElseThrow(() -> new RuntimeException(
                                                "Teacher not found"));

                return toResponse(teacher);
        }

        /**
         * Soft delete / deactivate teacher
         */
        @Transactional
        public void delete(Long id) {

                Teacher teacher = teacherRepository.findById(id)
                                .orElseThrow(() -> new RuntimeException(
                                                "Teacher not found"));

                User user = teacher.getUser();

                user.setStatus(UserStatus.INACTIVE);

                userRepository.save(user);
        }

        /**
         * Entity -> Response
         */
        private TeacherResponse toResponse(
                        Teacher teacher) {

                User user = teacher.getUser();

                return TeacherResponse.builder()
                                .id(teacher.getId())
                                .userId(user.getId())
                                .username(user.getUsername())
                                .name(teacher.getName())
                                .employeeCode(teacher.getEmployeeCode())
                                .phone(teacher.getPhone())
                                .email(teacher.getEmail())
                                .departmentId(teacher.getDepartment() != null
                                                ? teacher.getDepartment().getId()
                                                : null)
                                .departmentName(teacher.getDepartment() != null
                                                ? teacher.getDepartment().getName()
                                                : null)
                                .status(user.getStatus().name())
                                .build();
        }
}