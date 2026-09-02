package com.college.attendance.service;

import com.college.attendance.dto.section.SectionRequest;
import com.college.attendance.dto.section.SectionResponse;
import com.college.attendance.entity.Section;
import com.college.attendance.entity.Semester;
import com.college.attendance.repository.SectionRepository;
import com.college.attendance.repository.SemesterRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SectionService {

    private final SectionRepository sectionRepository;
    private final SemesterRepository semesterRepository;

    @Transactional
    public SectionResponse createOrUpdate(
            Long id,
            SectionRequest request
    ) {

        Section section;

        Semester semester =
                semesterRepository.findById(
                        request.getSemesterId()
                ).orElseThrow(() ->
                        new RuntimeException(
                                "Semester not found"
                        )
                );

        if (id == null) {

            if (sectionRepository
                    .existsBySemesterIdAndName(
                            request.getSemesterId(),
                            request.getName()
                    )) {

                throw new RuntimeException(
                        "Section already exists"
                );
            }

            section = new Section();

        } else {

            section = sectionRepository.findById(id)
                    .orElseThrow(() ->
                            new RuntimeException(
                                    "Section not found"
                            )
                    );
        }

        BeanUtils.copyProperties(
                request,
                section
        );

        section.setSemester(semester);

        section = sectionRepository.save(section);

        return toResponse(section);
    }

    @Transactional(readOnly = true)
    public List<SectionResponse> getAll() {

        return sectionRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public SectionResponse getById(Long id) {

        Section section =
                sectionRepository.findById(id)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Section not found"
                                )
                        );

        return toResponse(section);
    }

    @Transactional
    public void delete(Long id) {

        Section section =
                sectionRepository.findById(id)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Section not found"
                                )
                        );

        section.setStatus("INACTIVE");

        sectionRepository.save(section);
    }

    private SectionResponse toResponse(
            Section section
    ) {

        return SectionResponse.builder()
                .id(section.getId())
                .semesterId(
                        section.getSemester() != null
                                ? section.getSemester().getId()
                                : null
                )
                .name(section.getName())
                .status(section.getStatus())
                .build();
    }
}