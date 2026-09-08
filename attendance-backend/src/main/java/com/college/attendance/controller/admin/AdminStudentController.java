package com.college.attendance.controller.admin;

import com.college.attendance.dto.face.FaceRegistrationResponse;
import com.college.attendance.dto.student.StudentRequest;
import com.college.attendance.dto.student.StudentResponse;
import com.college.attendance.service.FaceRegistrationService;
import com.college.attendance.service.StudentService;

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
@RequestMapping("/api/admin/students")
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
public class AdminStudentController {

    private final StudentService studentService;
    private final FaceRegistrationService faceRegistrationService;

    // =====================================================
    // CREATE
    // =====================================================

    @PostMapping
    public ResponseEntity<StudentResponse> create(
            @Valid @RequestBody StudentRequest request
    ) {

        StudentResponse response =
                studentService.createOrUpdate(
                        null,
                        request
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    // =====================================================
    // GET ALL
    // =====================================================

    @GetMapping
    public ResponseEntity<List<StudentResponse>> getAll() {

        return ResponseEntity.ok(
                studentService.getAll()
        );
    }

    // =====================================================
    // GET BY ID
    // =====================================================

    @GetMapping("/{id}")
    public ResponseEntity<StudentResponse> getById(
            @PathVariable Long id
    ) {

        return ResponseEntity.ok(
                studentService.getById(id)
        );
    }

    // =====================================================
    // UPDATE
    // =====================================================

    @PutMapping("/{id}")
    public ResponseEntity<StudentResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody StudentRequest request
    ) {

        return ResponseEntity.ok(
                studentService.createOrUpdate(
                        id,
                        request
                )
        );
    }

    // =====================================================
    // DELETE / DEACTIVATE
    // =====================================================

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable Long id
    ) {

        studentService.delete(id);

        return ResponseEntity.noContent().build();
    }

    // =====================================================
    // PROFILE IMAGE
    // =====================================================

    @PostMapping(
            value = "/{studentId}/profile-image",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<StudentResponse> uploadProfileImage(
            @PathVariable Long studentId,
            @RequestParam("file") MultipartFile file
    ) throws Exception {

        return ResponseEntity.ok(
                studentService.uploadProfileImage(
                        studentId,
                        file
                )
        );
    }

    // =====================================================
    // FACE REGISTER
    // =====================================================

    @PostMapping(
            value = "/{userId}/face",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<FaceRegistrationResponse> registerFace(
            @PathVariable Long userId,
            @RequestParam("file") MultipartFile file
    ) throws Exception {

        return ResponseEntity.ok(
                faceRegistrationService.register(
                        userId,
                        file
                )
        );
    }
}