package com.college.attendance.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.college.attendance.entity.Holiday;
import com.college.attendance.entity.enums.Status;

public interface HolidayRepository extends JpaRepository<Holiday, Long> {

    List<Holiday> findByStatusOrderByHolidayDateAsc(
            Status status);

    List<Holiday> findByHolidayDateBetweenAndStatusOrderByHolidayDateAsc(
            LocalDate startDate,
            LocalDate endDate,
            Status status);

    Optional<Holiday> findByHolidayDate(
            LocalDate holidayDate);

    boolean existsByHolidayDate(
            LocalDate holidayDate);

    boolean existsByHolidayDateAndStatus(
            LocalDate holidayDate,
            Status status);
}