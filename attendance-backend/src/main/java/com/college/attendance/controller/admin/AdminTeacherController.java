package com.college.attendance.controller.admin;

import com.college.attendance.dto.teacher.TeacherRequest;
import com.college.attendance.dto.teacher.TeacherResponse;
import com.college.attendance.service.TeacherService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/teachers")
@RequiredArgsConstructor
public class AdminTeacherController {

    private final TeacherService teacherService;

    /**
     * Create Teacher
     */
    @PostMapping
    public ResponseEntity<TeacherResponse> create(
            @Valid @RequestBody TeacherRequest request
    ) {

        TeacherResponse response =
                teacherService.createOrUpdate(
                        null,
                        request
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    /**
     * Get All Teachers
     */
    @GetMapping
    public ResponseEntity<List<TeacherResponse>> getAll() {

        return ResponseEntity.ok(
                teacherService.getAll()
        );
    }

    /**
     * Get Teacher By ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<TeacherResponse> getById(
            @PathVariable Long id
    ) {

        return ResponseEntity.ok(
                teacherService.getById(id)
        );
    }

    /**
     * Update Teacher
     */
    @PutMapping("/{id}")
    public ResponseEntity<TeacherResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody TeacherRequest request
    ) {

        return ResponseEntity.ok(
                teacherService.createOrUpdate(
                        id,
                        request
                )
        );
    }

    /**
     * Soft Delete / Deactivate Teacher
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable Long id
    ) {

        teacherService.delete(id);

        return ResponseEntity.noContent().build();
    }
}