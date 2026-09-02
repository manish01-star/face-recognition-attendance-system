package com.college.attendance.dto.face;

import lombok.Data;

import java.util.List;

@Data
public class FaceServiceResponse {

    private boolean success;

    private boolean faceDetected;

    private int faceCount;

    private List<Double> embedding;

    private int embeddingSize;

    private String model;

    private String message;
}