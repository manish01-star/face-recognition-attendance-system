package com.college.attendance.controller.admin;

import com.college.attendance.dto.course.CourseRequest;
import com.college.attendance.dto.course.CourseResponse;
import com.college.attendance.service.CourseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/courses")
@RequiredArgsConstructor
public class AdminCourseController {

    private final CourseService courseService;

    @PostMapping
    public ResponseEntity<CourseResponse> create(
            @Valid @RequestBody CourseRequest request
    ) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        courseService.createOrUpdate(
                                null,
                                request
                        )
                );
    }

    @GetMapping
    public ResponseEntity<List<CourseResponse>> getAll() {

        return ResponseEntity.ok(
                courseService.getAll()
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<CourseResponse> getById(
            @PathVariable Long id
    ) {

        return ResponseEntity.ok(
                courseService.getById(id)
        );
    }

    @PutMapping("/{id}")
    public ResponseEntity<CourseResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody CourseRequest request
    ) {

        return ResponseEntity.ok(
                courseService.createOrUpdate(
                        id,
                        request
                )
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable Long id
    ) {

        courseService.delete(id);

        return ResponseEntity.noContent().build();
    }
}