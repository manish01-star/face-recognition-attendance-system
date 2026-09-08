package com.college.attendance.service;

import java.time.LocalDate;
import java.util.List;

import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.college.attendance.dto.holiday.HolidayRequest;
import com.college.attendance.dto.holiday.HolidayResponse;
import com.college.attendance.entity.Holiday;
import com.college.attendance.entity.enums.Status;
import com.college.attendance.repository.HolidayRepository;

@Service
public class HolidayService {

    private final HolidayRepository holidayRepository;

    public HolidayService(HolidayRepository holidayRepository) {
        this.holidayRepository = holidayRepository;
    }

    // =========================================================
    // CREATE HOLIDAY
    // =========================================================

    @Transactional
    public HolidayResponse createHoliday(HolidayRequest request) {

        validateRequest(request);

        LocalDate holidayDate = request.getHolidayDate();

        /*
         * Check whether an active holiday already exists
         * for the same date.
         */
        boolean exists =
                holidayRepository.existsByHolidayDateAndStatus(
                        holidayDate,
                        Status.ACTIVE
                );

        if (exists) {
            throw new RuntimeException(
                    "Holiday already exists for " + holidayDate
            );
        }

        Holiday holiday = new Holiday();

        holiday.setHolidayDate(holidayDate);

        holiday.setHolidayName(
                request.getHolidayName().trim()
        );

        holiday.setDescription(
                cleanDescription(request.getDescription())
        );

        holiday.setHolidayType(
                request.getHolidayType()
        );

        holiday.setStatus(
                Status.ACTIVE
        );

        Holiday savedHoliday =
                holidayRepository.save(holiday);

        return mapToResponse(savedHoliday);
    }

    // =========================================================
    // GET ACTIVE HOLIDAYS
    // =========================================================

    public List<HolidayResponse> getActiveHolidays() {

        return holidayRepository
                .findByStatusOrderByHolidayDateAsc(
                        Status.ACTIVE
                )
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    // =========================================================
    // GET ALL HOLIDAYS
    // =========================================================

    public List<HolidayResponse> getAllHolidays() {

        return holidayRepository
                .findAll()
                .stream()
                .sorted(
                        (a, b) ->
                                b.getHolidayDate()
                                        .compareTo(
                                                a.getHolidayDate()
                                        )
                )
                .map(this::mapToResponse)
                .toList();
    }

    // =========================================================
    // GET HOLIDAY BY ID
    // =========================================================

    public HolidayResponse getHolidayById(Long id) {

        Holiday holiday =
                getHolidayEntity(id);

        return mapToResponse(holiday);
    }

    // =========================================================
    // GET HOLIDAYS BY YEAR
    // =========================================================

    public List<HolidayResponse> getHolidaysByYear(int year) {

        if (year < 2000 || year > 2100) {
            throw new RuntimeException(
                    "Invalid year"
            );
        }

        LocalDate startDate =
                LocalDate.of(year, 1, 1);

        LocalDate endDate =
                LocalDate.of(year, 12, 31);

        return holidayRepository
                .findByHolidayDateBetweenAndStatusOrderByHolidayDateAsc(
                        startDate,
                        endDate,
                        Status.ACTIVE
                )
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    // =========================================================
    // GET HOLIDAY BY DATE
    // =========================================================

    public HolidayResponse getHolidayByDate(
            LocalDate date) {

        if (date == null) {
            throw new RuntimeException(
                    "Holiday date is required"
            );
        }

        Holiday holiday =
                holidayRepository
                        .findByHolidayDate(date)
                        .orElseThrow(
                                () -> new RuntimeException(
                                        "Holiday not found for "
                                                + date
                                )
                        );

        return mapToResponse(holiday);
    }

    // =========================================================
    // UPDATE HOLIDAY
    // =========================================================

    @Transactional
    public HolidayResponse updateHoliday(
            Long id,
            HolidayRequest request) {

        validateRequest(request);

        Holiday holiday =
                getHolidayEntity(id);

        LocalDate newDate =
                request.getHolidayDate();

        /*
         * Check duplicate date.
         *
         * Same holiday ID is ignored.
         */
        holidayRepository
                .findByHolidayDate(newDate)
                .ifPresent(existing -> {

                    if (!existing.getId().equals(id)
                            &&
                        existing.getStatus()
                                == Status.ACTIVE) {

                        throw new RuntimeException(
                                "Another active holiday already exists for "
                                        + newDate
                        );
                    }
                });

        holiday.setHolidayDate(newDate);

        holiday.setHolidayName(
                request.getHolidayName().trim()
        );

        holiday.setDescription(
                cleanDescription(
                        request.getDescription()
                )
        );

        holiday.setHolidayType(
                request.getHolidayType()
        );

        /*
         * Updating an inactive holiday
         * makes it active again.
         */
        holiday.setStatus(
                Status.ACTIVE
        );

        Holiday updatedHoliday =
                holidayRepository.save(holiday);

        return mapToResponse(updatedHoliday);
    }

    // =========================================================
    // SOFT DELETE
    // =========================================================

    @Transactional
    public void deleteHoliday(Long id) {

        Holiday holiday =
                getHolidayEntity(id);

        /*
         * Soft delete.
         *
         * Record remains in DB.
         */
        holiday.setStatus(
                Status.INACTIVE
        );

        holidayRepository.save(holiday);
    }

    // =========================================================
    // ACTIVATE HOLIDAY
    // =========================================================

    @Transactional
    public HolidayResponse activateHoliday(Long id) {

        Holiday holiday =
                getHolidayEntity(id);

        /*
         * Check whether another active holiday
         * exists on the same date.
         */
        holidayRepository
                .findByHolidayDate(
                        holiday.getHolidayDate()
                )
                .ifPresent(existing -> {

                    if (!existing.getId().equals(id)
                            &&
                        existing.getStatus()
                                == Status.ACTIVE) {

                        throw new RuntimeException(
                                "Another active holiday already exists for "
                                        + holiday.getHolidayDate()
                        );
                    }
                });

        holiday.setStatus(
                Status.ACTIVE
        );

        Holiday savedHoliday =
                holidayRepository.save(holiday);

        return mapToResponse(savedHoliday);
    }

    // =========================================================
    // VALIDATION
    // =========================================================

    private void validateRequest(
            HolidayRequest request) {

        if (request == null) {
            throw new RuntimeException(
                    "Holiday request is required"
            );
        }

        if (request.getHolidayDate() == null) {
            throw new RuntimeException(
                    "Holiday date is required"
            );
        }

        if (request.getHolidayName() == null
                ||
            request.getHolidayName()
                    .trim()
                    .isEmpty()) {

            throw new RuntimeException(
                    "Holiday name is required"
            );
        }

        if (request.getHolidayName()
                .trim()
                .length() > 150) {

            throw new RuntimeException(
                    "Holiday name cannot exceed 150 characters"
            );
        }

        if (request.getDescription() != null
                &&
            request.getDescription()
                    .trim()
                    .length() > 500) {

            throw new RuntimeException(
                    "Description cannot exceed 500 characters"
            );
        }

        if (request.getHolidayType() == null) {

            throw new RuntimeException(
                    "Holiday type is required"
            );
        }
    }

    // =========================================================
    // GET ENTITY
    // =========================================================

    private Holiday getHolidayEntity(Long id) {

        if (id == null) {
            throw new RuntimeException(
                    "Holiday ID is required"
            );
        }

        return holidayRepository
                .findById(id)
                .orElseThrow(
                        () -> new RuntimeException(
                                "Holiday not found"
                        )
                );
    }

    // =========================================================
    // ENTITY → RESPONSE
    // =========================================================

    private HolidayResponse mapToResponse(
            Holiday holiday) {

        HolidayResponse response =
                new HolidayResponse();

        BeanUtils.copyProperties(
                holiday,
                response
        );

        return response;
    }

    // =========================================================
    // CLEAN DESCRIPTION
    // =========================================================

    private String cleanDescription(
            String description) {

        if (description == null) {
            return null;
        }

        String value =
                description.trim();

        return value.isEmpty()
                ? null
                : value;
    }
}