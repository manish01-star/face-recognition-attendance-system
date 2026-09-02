package com.college.attendance.service;

import com.college.attendance.dto.course.CourseRequest;
import com.college.attendance.dto.course.CourseResponse;
import com.college.attendance.entity.Course;
import com.college.attendance.entity.Department;
import com.college.attendance.repository.CourseRepository;
import com.college.attendance.repository.DepartmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CourseService {

    private final CourseRepository courseRepository;
    private final DepartmentRepository departmentRepository;

    @Transactional
    public CourseResponse createOrUpdate(
            Long id,
            CourseRequest request
    ) {

        Course course;

        Department department =
                departmentRepository.findById(
                        request.getDepartmentId()
                ).orElseThrow(() ->
                        new RuntimeException(
                                "Department not found"
                        )
                );

        if (id == null) {

            if (courseRepository.existsByCode(
                    request.getCode())) {

                throw new RuntimeException(
                        "Course code already exists"
                );
            }

            course = new Course();

        } else {

            course = courseRepository.findById(id)
                    .orElseThrow(() ->
                            new RuntimeException(
                                    "Course not found"
                            )
                    );
        }

        BeanUtils.copyProperties(
                request,
                course
        );

        course.setDepartment(department);

        course = courseRepository.save(course);

        return toResponse(course);
    }

    @Transactional(readOnly = true)
    public List<CourseResponse> getAll() {

        return courseRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public CourseResponse getById(Long id) {

        Course course = courseRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Course not found"
                        )
                );

        return toResponse(course);
    }

    @Transactional
    public void delete(Long id) {

        Course course = courseRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Course not found"
                        )
                );

        course.setStatus("INACTIVE");

        courseRepository.save(course);
    }

    private CourseResponse toResponse(Course course) {

        return CourseResponse.builder()
                .id(course.getId())
                .name(course.getName())
                .code(course.getCode())
                .departmentId(
                        course.getDepartment() != null
                                ? course.getDepartment().getId()
                                : null
                )
                .status(course.getStatus())
                .build();
    }
}