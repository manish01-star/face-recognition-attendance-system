package com.college.attendance.dto.section;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SectionResponse {

    private Long id;

    private Long semesterId;

    private String name;

    private String status;
}