package com.college.attendance.controller.attendance;

import com.college.attendance.dto.attendance.AttendanceMarkResponse;
import com.college.attendance.dto.attendance.AttendanceResponse;
import com.college.attendance.service.AttendanceService;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/attendance")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class AttendanceController {

        private final AttendanceService attendanceService;

        /**
         * ============================================================
         * MOBILE - CHECK IN
         * ============================================================
         *
         * Logged-in user will be identified from JWT.
         *
         * Frontend will send:
         * 1. Face image
         * 2. Current latitude
         * 3. Current longitude
         */
        @PostMapping(value = "/check-in", consumes = "multipart/form-data")
        public ResponseEntity<AttendanceMarkResponse> checkIn(

                        @RequestParam("file") MultipartFile file,

                        @RequestParam("latitude") @NotNull Double latitude,

                        @RequestParam("longitude") @NotNull Double longitude

        ) throws Exception {

                return ResponseEntity.ok(
                                attendanceService.checkIn(
                                                file,
                                                latitude,
                                                longitude));
        }

        /**
         * ============================================================
         * MOBILE - CHECK OUT
         * ============================================================
         */
        @PostMapping(value = "/check-out", consumes = "multipart/form-data")
        public ResponseEntity<AttendanceMarkResponse> checkOut(

                        @RequestParam("file") MultipartFile file,

                        @RequestParam("latitude") @NotNull Double latitude,

                        @RequestParam("longitude") @NotNull Double longitude

        ) throws Exception {

                return ResponseEntity.ok(
                                attendanceService.checkOut(
                                                file,
                                                latitude,
                                                longitude));
        }

        /**
         * ============================================================
         * MOBILE - MY ATTENDANCE
         * ============================================================
         *
         * JWT se logged-in user identify hoga.
         */
        @GetMapping("/my")
        public ResponseEntity<List<AttendanceResponse>> getMyAttendance() {

                return ResponseEntity.ok(
                                attendanceService.getMyAttendance());
        }

        /**
         * ============================================================
         * MOBILE - MY ATTENDANCE BY DATE
         * ============================================================
         *
         * Calendar ke liye.
         */
        @GetMapping("/my/date/{date}")
        public ResponseEntity<AttendanceResponse> getMyAttendanceByDate(
                        @PathVariable LocalDate date) {

                AttendanceResponse response = attendanceService.getMyAttendanceByDate(date);

                if (response == null) {
                        return ResponseEntity.notFound().build();
                }

                return ResponseEntity.ok(response);
        }

}
