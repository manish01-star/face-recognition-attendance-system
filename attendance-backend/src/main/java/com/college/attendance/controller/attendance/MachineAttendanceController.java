package com.college.attendance.controller.attendance;

import com.college.attendance.dto.attendance.AttendanceMarkResponse;
import com.college.attendance.service.AttendanceService;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/attendance/machine")
@RequiredArgsConstructor
public class MachineAttendanceController {

    private final AttendanceService attendanceService;


    /**
     * ============================================================
     * COLLEGE MACHINE - CHECK IN
     * ============================================================
     *
     * Machine will send:
     *
     * 1. userId
     * 2. face image
     *
     * JWT is NOT required.
     *
     * Spring Boot will:
     *
     * 1. Find user
     * 2. Find registered face embedding
     * 3. Send image to Python anti-spoof service
     * 4. Send image + registered embedding to Python
     * 5. Verify face
     * 6. Mark attendance
     *
     * GPS is NOT required because machine is fixed
     * inside the college.
     */
    @PostMapping(
            value = "/check-in",
            consumes = "multipart/form-data"
    )
    public ResponseEntity<AttendanceMarkResponse> checkIn(

            @RequestParam("userId")
            Long userId,

            @RequestParam("file")
            MultipartFile file

    ) throws Exception {

        return ResponseEntity.ok(
                attendanceService.machineCheckIn(
                        userId,
                        file
                )
        );
    }


    /**
     * ============================================================
     * COLLEGE MACHINE - CHECK OUT
     * ============================================================
     *
     * Machine will send:
     *
     * 1. userId
     * 2. face image
     */
    @PostMapping(
            value = "/check-out",
            consumes = "multipart/form-data"
    )
    public ResponseEntity<AttendanceMarkResponse> checkOut(

            @RequestParam("userId")
            Long userId,

            @RequestParam("file")
            MultipartFile file

    ) throws Exception {

        return ResponseEntity.ok(
                attendanceService.machineCheckOut(
                        userId,
                        file
                )
        );
    }
}