package com.college.attendance.controller.admin;

import com.college.attendance.dto.face.FaceRegistrationResponse;
import com.college.attendance.dto.section.SectionRequest;
import com.college.attendance.dto.section.SectionResponse;
import com.college.attendance.service.SectionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/sections")
@RequiredArgsConstructor
public class AdminSectionController {

        private final SectionService sectionService;

        @PostMapping
        public ResponseEntity<SectionResponse> create(
                        @Valid @RequestBody SectionRequest request) {

                return ResponseEntity
                                .status(HttpStatus.CREATED)
                                .body(
                                                sectionService.createOrUpdate(
                                                                null,
                                                                request));
        }

        @GetMapping
        public ResponseEntity<List<SectionResponse>> getAll() {

                return ResponseEntity.ok(
                                sectionService.getAll());
        }

        @GetMapping("/{id}")
        public ResponseEntity<SectionResponse> getById(
                        @PathVariable Long id) {

                return ResponseEntity.ok(
                                sectionService.getById(id));
        }

        @PutMapping("/{id}")
        public ResponseEntity<SectionResponse> update(
                        @PathVariable Long id,
                        @Valid @RequestBody SectionRequest request) {

                return ResponseEntity.ok(
                                sectionService.createOrUpdate(
                                                id,
                                                request));
        }

        @DeleteMapping("/{id}")
        public ResponseEntity<Void> delete(
                        @PathVariable Long id) {

                sectionService.delete(id);

                return ResponseEntity.noContent().build();
        }
}