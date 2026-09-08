package com.college.attendance.controller.admin;

import com.college.attendance.dto.semester.SemesterRequest;
import com.college.attendance.dto.semester.SemesterResponse;
import com.college.attendance.service.SemesterService;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/semesters")
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
public class AdminSemesterController {

    private final SemesterService semesterService;

    @PostMapping
    public ResponseEntity<SemesterResponse> create(
            @Valid @RequestBody SemesterRequest request
    ) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        semesterService.createOrUpdate(
                                null,
                                request
                        )
                );
    }

    @GetMapping
    public ResponseEntity<List<SemesterResponse>> getAll() {

        return ResponseEntity.ok(
                semesterService.getAll()
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<SemesterResponse> getById(
            @PathVariable Long id
    ) {

        return ResponseEntity.ok(
                semesterService.getById(id)
        );
    }

    @PutMapping("/{id}")
    public ResponseEntity<SemesterResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody SemesterRequest request
    ) {

        return ResponseEntity.ok(
                semesterService.createOrUpdate(
                        id,
                        request
                )
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable Long id
    ) {

        semesterService.delete(id);

        return ResponseEntity.noContent().build();
    }
}