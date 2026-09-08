package com.college.attendance.service;

import com.college.attendance.dto.student.StudentRequest;
import com.college.attendance.dto.student.StudentResponse;
import com.college.attendance.entity.Course;
import com.college.attendance.entity.Section;
import com.college.attendance.entity.Semester;
import com.college.attendance.entity.Student;
import com.college.attendance.entity.User;
import com.college.attendance.entity.enums.Role;
import com.college.attendance.entity.enums.UserStatus;
import com.college.attendance.repository.CourseRepository;
import com.college.attendance.repository.SectionRepository;
import com.college.attendance.repository.SemesterRepository;
import com.college.attendance.repository.StudentRepository;
import com.college.attendance.repository.UserRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.beans.BeanUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import javax.imageio.ImageIO;

@Service
@RequiredArgsConstructor
public class StudentService {

    private final StudentRepository studentRepository;
    private final UserRepository userRepository;
    private final CourseRepository courseRepository;
    private final SemesterRepository semesterRepository;
    private final SectionRepository sectionRepository;
    private final PasswordEncoder passwordEncoder;

    // =====================================================
    // CREATE / UPDATE STUDENT
    // =====================================================

    /**
     * Create or Update Student
     *
     * id == null -> Create
     * id != null -> Update
     */
    @Transactional
    public StudentResponse createOrUpdate(
            Long id,
            StudentRequest request) {

        Student student;

        // =================================================
        // CREATE
        // =================================================

        if (id == null) {

            // ---------------------------------------------
            // Check Roll Number
            // ---------------------------------------------

            if (studentRepository.existsByRollNumber(
                    request.getRollNumber())) {

                throw new RuntimeException(
                        "Student with this roll number already exists");
            }

            // ---------------------------------------------
            // Check Username
            // Username = Roll Number
            // ---------------------------------------------

            if (userRepository.existsByUsername(
                    request.getRollNumber())) {

                throw new RuntimeException(
                        "Username already exists");
            }

            // ---------------------------------------------
            // Create User
            // ---------------------------------------------

            User user = User.builder()
                    .username(request.getRollNumber())
                    .password(
                            passwordEncoder.encode(
                                    request.getPhone()))
                    .role(Role.STUDENT)
                    .status(UserStatus.ACTIVE)
                    .build();

            userRepository.save(user);

            // ---------------------------------------------
            // Create Student
            // ---------------------------------------------

            student = new Student();

            student.setUser(user);

            BeanUtils.copyProperties(
                    request,
                    student);

            // ---------------------------------------------
            // Set Course / Semester / Section
            // ---------------------------------------------

            setRelationships(
                    student,
                    request.getCourseId(),
                    request.getSemesterId(),
                    request.getSectionId());
        }

        // =================================================
        // UPDATE
        // =================================================

        else {

            student = studentRepository.findById(id)
                    .orElseThrow(() ->
                            new RuntimeException(
                                    "Student not found"));

            User user = student.getUser();

            if (user == null) {
                throw new RuntimeException(
                        "User not found for student");
            }

            // ---------------------------------------------
            // Copy Student Fields
            // ---------------------------------------------

            BeanUtils.copyProperties(
                    request,
                    student);

            // ---------------------------------------------
            // Update Course / Semester / Section
            // ---------------------------------------------

            setRelationships(
                    student,
                    request.getCourseId(),
                    request.getSemesterId(),
                    request.getSectionId());

            // ---------------------------------------------
            // Phone Number = Password
            //
            // If phone number changes,
            // update password.
            // ---------------------------------------------

            if (!passwordEncoder.matches(
                    request.getPhone(),
                    user.getPassword())) {

                user.setPassword(
                        passwordEncoder.encode(
                                request.getPhone()));

                userRepository.save(user);
            }
        }

        // =================================================
        // SAVE STUDENT
        // =================================================

        student = studentRepository.save(student);

        return toResponse(student);
    }

    // =====================================================
    // SET RELATIONSHIPS
    // =====================================================

    private void setRelationships(
            Student student,
            Long courseId,
            Long semesterId,
            Long sectionId) {

        // ---------------------------------------------
        // Course
        // ---------------------------------------------

        Course course = courseRepository.findById(courseId)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Course not found"));

        // ---------------------------------------------
        // Semester
        // ---------------------------------------------

        Semester semester = semesterRepository.findById(semesterId)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Semester not found"));

        // ---------------------------------------------
        // Section
        // ---------------------------------------------

        Section section = sectionRepository.findById(sectionId)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Section not found"));

        // ---------------------------------------------
        // Set Relationships
        // ---------------------------------------------

        student.setCourse(course);
        student.setSemester(semester);
        student.setSection(section);
    }

    // =====================================================
    // UPLOAD / REPLACE PROFILE IMAGE
    // =====================================================

    /**
     * Upload / Replace Student Profile Image
     *
     * Actual image:
     * uploads/profiles/{userId}.jpg
     *
     * Database:
     * users.profile_image_url
     */
    @Transactional
    public StudentResponse uploadProfileImage(
            Long studentId,
            MultipartFile file) throws IOException {

        // ---------------------------------------------
        // Validate File
        // ---------------------------------------------

        if (file == null || file.isEmpty()) {

            throw new RuntimeException(
                    "Profile image is required");
        }

        // ---------------------------------------------
        // Validate File Size
        // ---------------------------------------------

        final long maxSize = 5 * 1024 * 1024; // 5 MB

        if (file.getSize() > maxSize) {

            throw new RuntimeException(
                    "Profile image size must not exceed 5 MB");
        }

        // ---------------------------------------------
        // Validate Content Type
        // ---------------------------------------------

        String contentType = file.getContentType();

        if (contentType == null ||
                !contentType.startsWith("image/")) {

            throw new RuntimeException(
                    "Only image files are allowed");
        }

        // ---------------------------------------------
        // Find Student
        // ---------------------------------------------

        Student student = studentRepository.findById(studentId)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Student not found"));

        // ---------------------------------------------
        // Get User
        // ---------------------------------------------

        User user = student.getUser();

        if (user == null) {

            throw new RuntimeException(
                    "User not found for student");
        }

        // ---------------------------------------------
        // Save Image
        // ---------------------------------------------

        String imageUrl =
                saveProfileImage(
                        user.getId(),
                        file);

        // ---------------------------------------------
        // Update User
        // ---------------------------------------------

        user.setProfileImageUrl(imageUrl);

        userRepository.save(user);

        // ---------------------------------------------
        // Return Updated Student
        // ---------------------------------------------

        return toResponse(student);
    }

    // =====================================================
    // SAVE PROFILE IMAGE TO DISK
    // =====================================================

    private String saveProfileImage(
            Long userId,
            MultipartFile file) throws IOException {

        // ---------------------------------------------
        // Upload Directory
        // ---------------------------------------------

        Path uploadDir =
                Paths.get("uploads/profiles");

        Files.createDirectories(uploadDir);

        // ---------------------------------------------
        // Read Image
        // ---------------------------------------------

        BufferedImage image =
                ImageIO.read(file.getInputStream());

        if (image == null) {

            throw new RuntimeException(
                    "Invalid image file");
        }

        // ---------------------------------------------
        // Convert Image To JPG
        // ---------------------------------------------

        ByteArrayOutputStream outputStream =
                new ByteArrayOutputStream();

        boolean written = ImageIO.write(
                image,
                "jpg",
                outputStream);

        if (!written) {

            throw new RuntimeException(
                    "Unable to process profile image");
        }

        // ---------------------------------------------
        // File Name
        // ---------------------------------------------

        String fileName =
                userId + ".jpg";

        Path filePath =
                uploadDir.resolve(fileName);

        // ---------------------------------------------
        // Save / Replace
        // ---------------------------------------------

        Files.write(
                filePath,
                outputStream.toByteArray());

        // ---------------------------------------------
        // Return URL
        // ---------------------------------------------

        return "/uploads/profiles/" + fileName;
    }

    // =====================================================
    // GET ALL STUDENTS
    // =====================================================

    @Transactional(readOnly = true)
    public List<StudentResponse> getAll() {

        return studentRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    // =====================================================
    // GET STUDENT BY ID
    // =====================================================

    @Transactional(readOnly = true)
    public StudentResponse getById(Long id) {

        Student student = studentRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Student not found"));

        return toResponse(student);
    }

    // =====================================================
    // SOFT DELETE
    // =====================================================

    @Transactional
    public void delete(Long id) {

        Student student = studentRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Student not found"));

        User user = student.getUser();

        if (user == null) {
            throw new RuntimeException(
                    "User not found for student");
        }

        user.setStatus(UserStatus.INACTIVE);

        userRepository.save(user);
    }

    // =====================================================
    // ENTITY -> RESPONSE DTO
    // =====================================================

    private StudentResponse toResponse(
            Student student) {

        User user = student.getUser();

        if (user == null) {
            throw new RuntimeException(
                    "User not found for student");
        }

        return StudentResponse.builder()

                .id(student.getId())

                .userId(user.getId())

                .username(user.getUsername())

                .name(student.getName())

                .rollNumber(student.getRollNumber())

                .phone(student.getPhone())

                .email(student.getEmail())

                // -----------------------------------------
                // Course
                // -----------------------------------------

                .courseId(
                        student.getCourse() != null
                                ? student.getCourse().getId()
                                : null)

                // -----------------------------------------
                // Semester
                // -----------------------------------------

                .semesterId(
                        student.getSemester() != null
                                ? student.getSemester().getId()
                                : null)

                // -----------------------------------------
                // Section
                // -----------------------------------------

                .sectionId(
                        student.getSection() != null
                                ? student.getSection().getId()
                                : null)

                // -----------------------------------------
                // User Status
                // -----------------------------------------

                .status(
                        user.getStatus() != null
                                ? user.getStatus().name()
                                : null)

                // -----------------------------------------
                // Profile Image
                // -----------------------------------------

                .profileImageUrl(
                        user.getProfileImageUrl())

                .build();
    }
}