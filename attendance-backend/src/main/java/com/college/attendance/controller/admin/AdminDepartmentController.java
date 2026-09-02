package com.college.attendance.controller.admin;

import com.college.attendance.dto.department.DepartmentRequest;
import com.college.attendance.dto.department.DepartmentResponse;
import com.college.attendance.service.DepartmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/departments")
@RequiredArgsConstructor
public class AdminDepartmentController {

    private final DepartmentService departmentService;

    @PostMapping
    public ResponseEntity<DepartmentResponse> create(
            @Valid @RequestBody DepartmentRequest request
    ) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        departmentService.createOrUpdate(
                                null,
                                request
                        )
                );
    }

    @GetMapping
    public ResponseEntity<List<DepartmentResponse>> getAll() {

        return ResponseEntity.ok(
                departmentService.getAll()
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<DepartmentResponse> getById(
            @PathVariable Long id
    ) {

        return ResponseEntity.ok(
                departmentService.getById(id)
        );
    }

    @PutMapping("/{id}")
    public ResponseEntity<DepartmentResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody DepartmentRequest request
    ) {

        return ResponseEntity.ok(
                departmentService.createOrUpdate(
                        id,
                        request
                )
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable Long id
    ) {

        departmentService.delete(id);

        return ResponseEntity.noContent().build();
    }
}