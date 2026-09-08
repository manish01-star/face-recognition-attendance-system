package com.college.attendance.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.college.attendance.dto.user.UserProfileResponse;
import com.college.attendance.entity.Student;
import com.college.attendance.entity.Teacher;
import com.college.attendance.entity.User;
import com.college.attendance.entity.enums.Role;
import com.college.attendance.repository.StudentRepository;
import com.college.attendance.repository.TeacherRepository;
import com.college.attendance.repository.UserRepository;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final StudentRepository studentRepository;
    private final TeacherRepository teacherRepository;

    public UserService(
            UserRepository userRepository,
            StudentRepository studentRepository,
            TeacherRepository teacherRepository) {

        this.userRepository = userRepository;
        this.studentRepository = studentRepository;
        this.teacherRepository = teacherRepository;
    }

    // =========================================================
    // USER - GET MY PROFILE
    // =========================================================

    @Transactional(readOnly = true)
    public UserProfileResponse getMyProfile(Long userId) {

        if (userId == null) {
            throw new RuntimeException("User ID is required");
        }

        User user = userRepository
                .findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        UserProfileResponse.UserProfileResponseBuilder response = UserProfileResponse.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .role(user.getRole() != null
                        ? user.getRole().name()
                        : null)
                .profileImageUrl(user.getProfileImageUrl());

        if (user.getRole() == Role.STUDENT) {

            Student student = studentRepository
                    .findProfileByUserId(userId)
                    .orElseThrow(() -> new RuntimeException("Student profile not found"));

            response
                    .name(student.getName())
                    .email(student.getEmail())
                    .phone(student.getPhone())
                    .rollNumber(student.getRollNumber());

        } else if (user.getRole() == Role.TEACHER) {

            Teacher teacher = teacherRepository
                    .findProfileByUserId(userId)
                    .orElseThrow(() -> new RuntimeException("Teacher profile not found"));

            response
                    .name(teacher.getName())
                    .email(teacher.getEmail())
                    .phone(teacher.getPhone())
                    .employeeNumber(teacher.getEmployeeCode());
        }

        return response.build();
    }

}