package com.college.attendance.service;

import com.college.attendance.dto.semester.SemesterRequest;
import com.college.attendance.dto.semester.SemesterResponse;
import com.college.attendance.entity.Course;
import com.college.attendance.entity.Semester;
import com.college.attendance.repository.CourseRepository;
import com.college.attendance.repository.SemesterRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SemesterService {

    private final SemesterRepository semesterRepository;
    private final CourseRepository courseRepository;

    @Transactional
    public SemesterResponse createOrUpdate(
            Long id,
            SemesterRequest request
    ) {

        Semester semester;

        Course course = courseRepository.findById(
                request.getCourseId()
        ).orElseThrow(() ->
                new RuntimeException(
                        "Course not found"
                )
        );

        if (id == null) {

            if (semesterRepository
                    .existsByCourseIdAndSemesterNumber(
                            request.getCourseId(),
                            request.getSemesterNumber()
                    )) {

                throw new RuntimeException(
                        "Semester already exists for this course"
                );
            }

            semester = new Semester();

        } else {

            semester = semesterRepository.findById(id)
                    .orElseThrow(() ->
                            new RuntimeException(
                                    "Semester not found"
                            )
                    );
        }

        BeanUtils.copyProperties(
                request,
                semester
        );

        semester.setCourse(course);

        semester = semesterRepository.save(semester);

        return toResponse(semester);
    }

    @Transactional(readOnly = true)
    public List<SemesterResponse> getAll() {

        return semesterRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public SemesterResponse getById(Long id) {

        Semester semester =
                semesterRepository.findById(id)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Semester not found"
                                )
                        );

        return toResponse(semester);
    }

    @Transactional
    public void delete(Long id) {

        Semester semester =
                semesterRepository.findById(id)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Semester not found"
                                )
                        );

        semester.setStatus("INACTIVE");

        semesterRepository.save(semester);
    }

    private SemesterResponse toResponse(
            Semester semester
    ) {

        return SemesterResponse.builder()
                .id(semester.getId())
                .courseId(
                        semester.getCourse() != null
                                ? semester.getCourse().getId()
                                : null
                )
                .semesterNumber(
                        semester.getSemesterNumber()
                )
                .status(semester.getStatus())
                .build();
    }
}