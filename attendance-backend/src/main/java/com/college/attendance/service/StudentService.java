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

import java.util.List;

@Service
@RequiredArgsConstructor
public class StudentService {

    private final StudentRepository studentRepository;
    private final UserRepository userRepository;
    private final CourseRepository courseRepository;
    private final SemesterRepository semesterRepository;
    private final SectionRepository sectionRepository;
    private final PasswordEncoder passwordEncoder;

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

        // ==========================================
        // CREATE
        // ==========================================

        if (id == null) {

            if (studentRepository.existsByRollNumber(
                    request.getRollNumber())) {

                throw new RuntimeException(
                        "Student with this roll number already exists");
            }

            if (userRepository.existsByUsername(
                    request.getRollNumber())) {

                throw new RuntimeException(
                        "Username already exists");
            }

            User user = User.builder()
                    .username(request.getRollNumber())
                    .password(
                            passwordEncoder.encode(
                                    request.getPhone()))
                    .role(Role.STUDENT)
                    .status(UserStatus.ACTIVE)
                    .build();

            userRepository.save(user);

            student = new Student();

            student.setUser(user);

            BeanUtils.copyProperties(
                    request,
                    student);

            setRelationships(
                    student,
                    request.getCourseId(),
                    request.getSemesterId(),
                    request.getSectionId());

        }

        // ==========================================
        // UPDATE
        // ==========================================

        else {

            student = studentRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException(
                            "Student not found"));

            User user = student.getUser();

            BeanUtils.copyProperties(
                    request,
                    student);

            setRelationships(
                    student,
                    request.getCourseId(),
                    request.getSemesterId(),
                    request.getSectionId());

            /*
             * Phone number is the initial password.
             * Therefore if phone changes,
             * update password as well.
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

        student = studentRepository.save(student);

        return toResponse(student);
    }

    /**
     * Set Course, Semester and Section
     */
    private void setRelationships(
            Student student,
            Long courseId,
            Long semesterId,
            Long sectionId) {

        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new RuntimeException(
                        "Course not found"));

        Semester semester = semesterRepository.findById(semesterId)
                .orElseThrow(() -> new RuntimeException(
                        "Semester not found"));

        Section section = sectionRepository.findById(sectionId)
                .orElseThrow(() -> new RuntimeException(
                        "Section not found"));

        student.setCourse(course);
        student.setSemester(semester);
        student.setSection(section);
    }

    /**
     * Get all students
     */
    @Transactional(readOnly = true)
    public List<StudentResponse> getAll() {

        return studentRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * Get student by ID
     */
    @Transactional(readOnly = true)
    public StudentResponse getById(Long id) {

        Student student = studentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException(
                        "Student not found"));

        return toResponse(student);
    }

    /**
     * Soft delete / deactivate student
     */
    @Transactional
    public void delete(Long id) {

        Student student = studentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException(
                        "Student not found"));

        User user = student.getUser();

        user.setStatus(UserStatus.INACTIVE);

        userRepository.save(user);
    }

    /**
     * Convert Entity -> Response DTO
     */
    private StudentResponse toResponse(Student student) {

        User user = student.getUser();

        return StudentResponse.builder()
                .id(student.getId())
                .userId(user.getId())
                .username(user.getUsername())
                .name(student.getName())
                .rollNumber(student.getRollNumber())
                .phone(student.getPhone())
                .email(student.getEmail())
                .courseId(
                        student.getCourse() != null
                                ? student.getCourse().getId()
                                : null)
                .semesterId(
                        student.getSemester() != null
                                ? student.getSemester().getId()
                                : null)
                .sectionId(
                        student.getSection() != null
                                ? student.getSection().getId()
                                : null)
                .status(
                        user.getStatus().name())
                .build();
    }
}