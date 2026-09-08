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
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TeacherService {

    private final TeacherRepository teacherRepository;

    private final UserRepository userRepository;

    private final DepartmentRepository departmentRepository;

    private final PasswordEncoder passwordEncoder;


    // =====================================================
    // CREATE / UPDATE TEACHER
    // =====================================================

    /**
     * Create or Update Teacher
     *
     * id == null -> Create
     * id != null -> Update
     *
     * Profile image is optional for update.
     */
    @Transactional
    public TeacherResponse createOrUpdate(
            Long id,
            TeacherRequest request,
            MultipartFile file
    ) throws IOException {

        Teacher teacher;


        // =================================================
        // CREATE
        // =================================================

        if (id == null) {

            // ---------------------------------------------
            // Check Employee Code
            // ---------------------------------------------

            if (teacherRepository.existsByEmployeeCode(
                    request.getEmployeeCode())) {

                throw new RuntimeException(
                        "Teacher with this employee code already exists"
                );
            }


            // ---------------------------------------------
            // Check Username
            //
            // Username = Employee Code
            // ---------------------------------------------

            if (userRepository.existsByUsername(
                    request.getEmployeeCode())) {

                throw new RuntimeException(
                        "Username already exists"
                );
            }


            // ---------------------------------------------
            // Department
            // ---------------------------------------------

            Department department =
                    departmentRepository.findById(
                            request.getDepartmentId()
                    ).orElseThrow(
                            () -> new RuntimeException(
                                    "Department not found"
                            )
                    );


            // ---------------------------------------------
            // Create User
            // ---------------------------------------------

            User user = User.builder()
                    .username(
                            request.getEmployeeCode()
                    )
                    .password(
                            passwordEncoder.encode(
                                    request.getPhone()
                            )
                    )
                    .role(Role.TEACHER)
                    .status(UserStatus.ACTIVE)
                    .build();

            userRepository.save(user);


            // ---------------------------------------------
            // Create Teacher
            // ---------------------------------------------

            teacher = new Teacher();

            teacher.setUser(user);

            BeanUtils.copyProperties(
                    request,
                    teacher
            );

            teacher.setDepartment(department);
        }


        // =================================================
        // UPDATE
        // =================================================

        else {

            teacher = teacherRepository.findById(id)
                    .orElseThrow(
                            () -> new RuntimeException(
                                    "Teacher not found"
                            )
                    );


            User user = teacher.getUser();


            if (user == null) {

                throw new RuntimeException(
                        "User not found for teacher"
                );
            }


            // ---------------------------------------------
            // Department
            // ---------------------------------------------

            Department department =
                    departmentRepository.findById(
                            request.getDepartmentId()
                    ).orElseThrow(
                            () -> new RuntimeException(
                                    "Department not found"
                            )
                    );


            // ---------------------------------------------
            // Copy Teacher Fields
            // ---------------------------------------------

            BeanUtils.copyProperties(
                    request,
                    teacher
            );


            teacher.setDepartment(department);


            // ---------------------------------------------
            // Phone Number = Password
            //
            // Update password if phone changed
            // ---------------------------------------------

            if (!passwordEncoder.matches(
                    request.getPhone(),
                    user.getPassword()
            )) {

                user.setPassword(
                        passwordEncoder.encode(
                                request.getPhone()
                        )
                );

                userRepository.save(user);
            }
        }


        // =================================================
        // SAVE TEACHER
        // =================================================

        teacher =
                teacherRepository.save(teacher);


        // =================================================
        // SAVE PROFILE IMAGE
        // =================================================

        if (file != null && !file.isEmpty()) {

            User user = teacher.getUser();

            String imageUrl =
                    saveProfileImage(
                            user.getId(),
                            file
                    );

            user.setProfileImageUrl(
                    imageUrl
            );

            userRepository.save(user);
        }


        // =================================================
        // RESPONSE
        // =================================================

        return toResponse(teacher);
    }


    // =====================================================
    // SAVE PROFILE IMAGE
    // =====================================================

    /**
     * Save teacher profile image.
     *
     * Actual file:
     *
     * uploads/profiles/{userId}.jpg
     *
     * Database:
     *
     * users.profile_image_url
     */
    private String saveProfileImage(
            Long userId,
            MultipartFile file
    ) throws IOException {


        // ---------------------------------------------
        // Validate file
        // ---------------------------------------------

        if (file == null || file.isEmpty()) {

            throw new RuntimeException(
                    "Profile image is empty"
            );
        }


        // ---------------------------------------------
        // Validate content type
        // ---------------------------------------------

        String contentType =
                file.getContentType();

        if (contentType == null ||
                !contentType.startsWith("image/")) {

            throw new RuntimeException(
                    "Only image files are allowed"
            );
        }


        // ---------------------------------------------
        // Optional file size validation
        // 5 MB
        // ---------------------------------------------

        long maxSize =
                5 * 1024 * 1024;

        if (file.getSize() > maxSize) {

            throw new RuntimeException(
                    "Profile image size must be less than 5 MB"
            );
        }


        // ---------------------------------------------
        // Create directory
        // ---------------------------------------------

        Path uploadDir =
                Paths.get(
                        "uploads/profiles"
                );

        Files.createDirectories(
                uploadDir
        );


        // ---------------------------------------------
        // File name
        //
        // User ID prevents unsafe filenames
        // ---------------------------------------------

        String fileName =
                userId + ".jpg";


        Path filePath =
                uploadDir.resolve(
                        fileName
                );


        // ---------------------------------------------
        // Save / Replace
        // ---------------------------------------------

        Files.write(
                filePath,
                file.getBytes()
        );


        // ---------------------------------------------
        // URL
        // ---------------------------------------------

        return "/uploads/profiles/"
                + fileName;
    }


    // =====================================================
    // GET ALL TEACHERS
    // =====================================================

    @Transactional(readOnly = true)
    public List<TeacherResponse> getAll() {

        return teacherRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }


    // =====================================================
    // GET TEACHER BY ID
    // =====================================================

    @Transactional(readOnly = true)
    public TeacherResponse getById(
            Long id
    ) {

        Teacher teacher =
                teacherRepository.findById(id)
                        .orElseThrow(
                                () -> new RuntimeException(
                                        "Teacher not found"
                                )
                        );

        return toResponse(teacher);
    }


    // =====================================================
    // SOFT DELETE
    // =====================================================

    @Transactional
    public void delete(
            Long id
    ) {

        Teacher teacher =
                teacherRepository.findById(id)
                        .orElseThrow(
                                () -> new RuntimeException(
                                        "Teacher not found"
                                )
                        );


        User user =
                teacher.getUser();


        if (user == null) {

            throw new RuntimeException(
                    "User not found for teacher"
            );
        }


        user.setStatus(
                UserStatus.INACTIVE
        );

        userRepository.save(user);
    }


    // =====================================================
    // ENTITY -> RESPONSE
    // =====================================================

    private TeacherResponse toResponse(
            Teacher teacher
    ) {

        User user =
                teacher.getUser();


        if (user == null) {

            throw new RuntimeException(
                    "User not found for teacher"
            );
        }


        return TeacherResponse.builder()

                .id(
                        teacher.getId()
                )

                .userId(
                        user.getId()
                )

                .username(
                        user.getUsername()
                )

                .name(
                        teacher.getName()
                )

                .employeeCode(
                        teacher.getEmployeeCode()
                )

                .phone(
                        teacher.getPhone()
                )

                .email(
                        teacher.getEmail()
                )

                .departmentId(
                        teacher.getDepartment() != null
                                ? teacher.getDepartment().getId()
                                : null
                )

                .departmentName(
                        teacher.getDepartment() != null
                                ? teacher.getDepartment().getName()
                                : null
                )

                .status(
                        user.getStatus().name()
                )

                .profileImageUrl(
                        user.getProfileImageUrl()
                )

                .build();
    }
}