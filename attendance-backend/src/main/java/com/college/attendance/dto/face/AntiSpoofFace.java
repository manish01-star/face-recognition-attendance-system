package com.college.attendance.dto.face;

import lombok.Data;

@Data
public class AntiSpoofFace {

    private boolean isReal;

    private double antiSpoofScore;

    private FaceArea face;
}