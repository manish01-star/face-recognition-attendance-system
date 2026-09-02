package com.college.attendance.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * ============================================================
     * FACE VERIFICATION ERROR
     * ============================================================
     *
     * 422 = Request valid hai,
     * but face verification / anti-spoof failed.
     */
    @ExceptionHandler(FaceVerificationException.class)
    public ResponseEntity<Map<String, Object>> handleFaceVerificationException(
            FaceVerificationException ex) {

        log.warn("Face verification failed: {}", ex.getMessage());

        return buildResponse(
                HttpStatus.UNPROCESSABLE_ENTITY,
                ex.getMessage()
        );
    }


    /**
     * ============================================================
     * ATTENDANCE BUSINESS / VALIDATION ERROR
     * ============================================================
     *
     * 400 = Client ne invalid operation/request ki.
     */
    @ExceptionHandler(AttendanceException.class)
    public ResponseEntity<Map<String, Object>> handleAttendanceException(
            AttendanceException ex) {

        log.warn("Attendance error: {}", ex.getMessage());

        return buildResponse(
                HttpStatus.BAD_REQUEST,
                ex.getMessage()
        );
    }


    /**
     * ============================================================
     * RESOURCE NOT FOUND
     * ============================================================
     *
     * 404 = User / Attendance / Face etc. nahi mila.
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleResourceNotFoundException(
            ResourceNotFoundException ex) {

        log.warn("Resource not found: {}", ex.getMessage());

        return buildResponse(
                HttpStatus.NOT_FOUND,
                ex.getMessage()
        );
    }


    /**
     * ============================================================
     * BAD CREDENTIALS
     * ============================================================
     */
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<Map<String, Object>> handleBadCredentials(
            BadCredentialsException ex) {

        return buildResponse(
                HttpStatus.UNAUTHORIZED,
                "Invalid username or password"
        );
    }


    /**
     * ============================================================
     * ILLEGAL ARGUMENT
     * ============================================================
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(
            IllegalArgumentException ex) {

        log.warn("Invalid argument: {}", ex.getMessage());

        return buildResponse(
                HttpStatus.BAD_REQUEST,
                ex.getMessage()
        );
    }


    /**
     * ============================================================
     * UNEXPECTED ERROR
     * ============================================================
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(
            Exception ex) {

        log.error("Unexpected server error", ex);

        return buildResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Something went wrong. Please try again later."
        );
    }


    /**
     * ============================================================
     * COMMON RESPONSE
     * ============================================================
     */
    private ResponseEntity<Map<String, Object>> buildResponse(
            HttpStatus status,
            String message) {

        Map<String, Object> response = new HashMap<>();

        response.put("success", false);
        response.put("message",
                message != null && !message.isBlank()
                        ? message
                        : "Something went wrong");

        return ResponseEntity
                .status(status)
                .body(response);
    }
}