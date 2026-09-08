package com.college.attendance.controller.admin;

import com.college.attendance.dto.teacher.TeacherRequest;
import com.college.attendance.dto.teacher.TeacherResponse;
import com.college.attendance.service.TeacherService;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/admin/teachers")
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
public class AdminTeacherController {

    private final TeacherService teacherService;

    // =====================================================
    // CREATE TEACHER
    // =====================================================

    /**
     * Create Teacher
     *
     * Multipart:
     *
     * data -> TeacherRequest JSON
     * file -> Profile Image
     */
    @PostMapping(
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<TeacherResponse> create(

            @RequestPart("data")
            @Valid
            TeacherRequest request,

            @RequestPart(value = "file", required = false)
            MultipartFile file

    ) throws Exception {

        TeacherResponse response =
                teacherService.createOrUpdate(
                        null,
                        request,
                        file
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    // =====================================================
    // GET ALL TEACHERS
    // =====================================================

    @GetMapping
    public ResponseEntity<List<TeacherResponse>> getAll() {

        return ResponseEntity.ok(
                teacherService.getAll()
        );
    }

    // =====================================================
    // GET TEACHER BY ID
    // =====================================================

    @GetMapping("/{id}")
    public ResponseEntity<TeacherResponse> getById(
            @PathVariable Long id
    ) {

        return ResponseEntity.ok(
                teacherService.getById(id)
        );
    }

    // =====================================================
    // UPDATE TEACHER
    // =====================================================

    /**
     * Update Teacher
     *
     * file is optional.
     *
     * If new file is provided:
     *     old profile image will be replaced.
     *
     * If file is not provided:
     *     existing profile image remains unchanged.
     */
    @PutMapping(
            value = "/{id}",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<TeacherResponse> update(

            @PathVariable Long id,

            @RequestPart("data")
            @Valid
            TeacherRequest request,

            @RequestPart(value = "file", required = false)
            MultipartFile file

    ) throws Exception {

        return ResponseEntity.ok(
                teacherService.createOrUpdate(
                        id,
                        request,
                        file
                )
        );
    }

    // =====================================================
    // SOFT DELETE / DEACTIVATE
    // =====================================================

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable Long id
    ) {

        teacherService.delete(id);

        return ResponseEntity
                .noContent()
                .build();
    }
}