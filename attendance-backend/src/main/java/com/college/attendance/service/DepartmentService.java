package com.college.attendance.service;

import com.college.attendance.dto.department.DepartmentRequest;
import com.college.attendance.dto.department.DepartmentResponse;
import com.college.attendance.entity.Department;
import com.college.attendance.repository.DepartmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DepartmentService {

    private final DepartmentRepository departmentRepository;

    @Transactional
    public DepartmentResponse createOrUpdate(
            Long id,
            DepartmentRequest request
    ) {

        Department department;

        if (id == null) {

            if (departmentRepository.existsByCode(
                    request.getCode())) {

                throw new RuntimeException(
                        "Department code already exists"
                );
            }

            department = new Department();

            BeanUtils.copyProperties(
                    request,
                    department
            );

        } else {

            department = departmentRepository.findById(id)
                    .orElseThrow(() ->
                            new RuntimeException(
                                    "Department not found"
                            )
                    );

            BeanUtils.copyProperties(
                    request,
                    department
            );
        }

        department = departmentRepository.save(department);

        return toResponse(department);
    }

    @Transactional(readOnly = true)
    public List<DepartmentResponse> getAll() {

        return departmentRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public DepartmentResponse getById(Long id) {

        Department department =
                departmentRepository.findById(id)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Department not found"
                                )
                        );

        return toResponse(department);
    }

    @Transactional
    public void delete(Long id) {

        Department department =
                departmentRepository.findById(id)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Department not found"
                                )
                        );

        department.setStatus("INACTIVE");

        departmentRepository.save(department);
    }

    private DepartmentResponse toResponse(
            Department department
    ) {

        return DepartmentResponse.builder()
                .id(department.getId())
                .name(department.getName())
                .code(department.getCode())
                .status(department.getStatus())
                .build();
    }
}