package com.college.attendance.controller.admin;

import com.college.attendance.dto.face.FaceRegistrationResponse;
import com.college.attendance.service.FaceRegistrationService;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/admin/faces")
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
public class AdminFaceController {

    private final FaceRegistrationService faceRegistrationService;


    @PostMapping(
            value = "/register/{userId}",
            consumes = "multipart/form-data"
    )
    public ResponseEntity<FaceRegistrationResponse> registerFace(

            @PathVariable Long userId,

            @RequestParam("file")
            MultipartFile file

    ) throws Exception {

        return ResponseEntity.ok(
                faceRegistrationService.register(
                        userId,
                        file
                )
        );
    }
}