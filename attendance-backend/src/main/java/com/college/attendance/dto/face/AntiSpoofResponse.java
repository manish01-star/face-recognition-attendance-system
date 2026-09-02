package com.college.attendance.dto.face;

import lombok.Data;

import java.util.List;

@Data
public class AntiSpoofResponse {

    private boolean success;

    private boolean faceDetected;

    private int faceCount;

    private boolean live;

    private List<AntiSpoofFace> faces;

    private String message;
}