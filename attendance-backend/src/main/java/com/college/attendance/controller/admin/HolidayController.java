package com.college.attendance.controller.admin;

import java.time.LocalDate;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.college.attendance.dto.holiday.HolidayRequest;
import com.college.attendance.dto.holiday.HolidayResponse;
import com.college.attendance.service.HolidayService;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;

@RestController
@RequestMapping("/api/holidays")
@SecurityRequirement(name = "bearerAuth")
public class HolidayController {

    private final HolidayService holidayService;

    public HolidayController(
            HolidayService holidayService) {

        this.holidayService =
                holidayService;
    }

    // =========================================================
    // CREATE HOLIDAY
    // =========================================================

    @PostMapping
    public ResponseEntity<HolidayResponse> createHoliday(
            @RequestBody HolidayRequest request) {

        HolidayResponse response =
                holidayService.createHoliday(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    // =========================================================
    // GET ACTIVE HOLIDAYS
    // =========================================================

    @GetMapping
    public ResponseEntity<List<HolidayResponse>>
    getActiveHolidays() {

        return ResponseEntity.ok(
                holidayService.getActiveHolidays()
        );
    }

    // =========================================================
    // GET ALL HOLIDAYS - ADMIN
    // =========================================================

    @GetMapping("/admin")
    public ResponseEntity<List<HolidayResponse>>
    getAllHolidays() {

        return ResponseEntity.ok(
                holidayService.getAllHolidays()
        );
    }

    // =========================================================
    // GET HOLIDAYS BY YEAR
    // =========================================================

    @GetMapping("/year/{year}")
    public ResponseEntity<List<HolidayResponse>>
    getHolidaysByYear(
            @PathVariable int year) {

        return ResponseEntity.ok(
                holidayService.getHolidaysByYear(year)
        );
    }

    // =========================================================
    // GET HOLIDAY BY DATE
    // =========================================================

    @GetMapping("/date/{date}")
    public ResponseEntity<HolidayResponse>
    getHolidayByDate(
            @PathVariable LocalDate date) {

        return ResponseEntity.ok(
                holidayService.getHolidayByDate(date)
        );
    }

    // =========================================================
    // GET HOLIDAY BY ID
    // =========================================================

    @GetMapping("/{id}")
    public ResponseEntity<HolidayResponse>
    getHolidayById(
            @PathVariable Long id) {

        return ResponseEntity.ok(
                holidayService.getHolidayById(id)
        );
    }

    // =========================================================
    // UPDATE HOLIDAY
    // =========================================================

    @PutMapping("/{id}")
    public ResponseEntity<HolidayResponse>
    updateHoliday(
            @PathVariable Long id,
            @RequestBody HolidayRequest request) {

        return ResponseEntity.ok(
                holidayService.updateHoliday(
                        id,
                        request
                )
        );
    }

    // =========================================================
    // SOFT DELETE
    // =========================================================

    @DeleteMapping("/{id}")
    public ResponseEntity<Void>
    deleteHoliday(
            @PathVariable Long id) {

        holidayService.deleteHoliday(id);

        return ResponseEntity
                .noContent()
                .build();
    }

    // =========================================================
    // ACTIVATE
    // =========================================================

    @PutMapping("/{id}/activate")
    public ResponseEntity<HolidayResponse>
    activateHoliday(
            @PathVariable Long id) {

        return ResponseEntity.ok(
                holidayService.activateHoliday(id)
        );
    }
}