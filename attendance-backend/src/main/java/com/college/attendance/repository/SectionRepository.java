package com.college.attendance.repository;

import com.college.attendance.entity.Section;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SectionRepository extends JpaRepository<Section, Long> {

    List<Section> findBySemesterId(Long semesterId);

    Optional<Section> findBySemesterIdAndName(
            Long semesterId,
            String name
    );

    boolean existsBySemesterIdAndName(
            Long semesterId,
            String name
    );
}